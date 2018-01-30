/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifact;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifactHash;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadResponse;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken;
import org.eclipse.hawkbit.security.DmfTenantSecurityToken.FileResource;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * {@link AmqpMessageHandlerService} handles all incoming target authentication
 * AMQP messages that can be used by 3rd party CDN services to check if a target
 * is permitted to download certain artifact. This is handled by the queue that
 * is configured for the property
 * hawkbit.dmf.rabbitmq.authenticationReceiverQueue.
 *
 */
public class AmqpAuthenticationMessageHandler extends BaseAmqpService {
    private static final Logger LOG = LoggerFactory.getLogger(AmqpAuthenticationMessageHandler.class);

    private final AmqpControllerAuthentication authenticationManager;

    private final ArtifactManagement artifactManagement;

    private final DownloadIdCache cache;

    private final HostnameResolver hostnameResolver;

    private final ControllerManagement controllerManagement;

    private final TenantAware tenantAware;

    /**
     * @param rabbitTemplate
     *            the configured amqp template.
     * @param artifactManagement
     *            for artifact URI generation
     * @param cache
     *            for download Ids
     * @param hostnameResolver
     *            for resolving the host for downloads
     * @param authenticationManager
     *            for target authentication
     * @param controllerManagement
     *            for target repo access
     * @param tenantAware
     *            to access current tenant
     */
    public AmqpAuthenticationMessageHandler(final RabbitTemplate rabbitTemplate,
            final AmqpControllerAuthentication authenticationManager, final ArtifactManagement artifactManagement,
            final DownloadIdCache cache, final HostnameResolver hostnameResolver,
            final ControllerManagement controllerManagement, final TenantAware tenantAware) {
        super(rabbitTemplate);
        this.authenticationManager = authenticationManager;
        this.artifactManagement = artifactManagement;
        this.cache = cache;
        this.hostnameResolver = hostnameResolver;
        this.controllerManagement = controllerManagement;
        this.tenantAware = tenantAware;
    }

    /**
     * Executed on an authentication request.
     * 
     * @param message
     *            the amqp message
     * @return the rpc message back to supplier.
     */
    @RabbitListener(queues = "${hawkbit.dmf.rabbitmq.authenticationReceiverQueue:authentication_receiver}", containerFactory = "listenerContainerFactory")
    public Message onAuthenticationRequest(final Message message) {
        checkContentTypeJson(message);
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            return handleAuthenticationMessage(message);
        } catch (final RuntimeException ex) {
            throw new AmqpRejectAndDontRequeueException(ex);
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    /**
     * check action for this download purposes, the method will throw an
     * EntityNotFoundException in case the controller is not allowed to download
     * this file because it's not assigned to an action and not assigned to this
     * controller. Otherwise no controllerId is set = anonymous download
     * 
     * @param secruityToken
     *            the security token which holds the target ID to check on
     * @param sha1Hash
     *            of the artifact to verify if the given target is allowed to
     *            download it
     */
    private void checkIfArtifactIsAssignedToTarget(final DmfTenantSecurityToken secruityToken, final String sha1Hash) {

        if (secruityToken.getControllerId() != null) {
            checkByControllerId(sha1Hash, secruityToken.getControllerId());
        } else if (secruityToken.getTargetId() != null) {
            checkByTargetId(sha1Hash, secruityToken.getTargetId());
        } else {
            LOG.info("anonymous download no authentication check for artifact {}", sha1Hash);
            return;
        }

    }

    private void checkByTargetId(final String sha1Hash, final Long targetId) {
        LOG.debug("no anonymous download request, doing authentication check for target {} and artifact {}", targetId,
                sha1Hash);
        if (!controllerManagement.hasTargetArtifactAssigned(targetId, sha1Hash)) {
            LOG.info("target {} tried to download artifact {} which is not assigned to the target", targetId, sha1Hash);
            throw new EntityNotFoundException();
        }
        LOG.info("download security check for target {} and artifact {} granted", targetId, sha1Hash);
    }

    private void checkByControllerId(final String sha1Hash, final String controllerId) {
        LOG.debug("no anonymous download request, doing authentication check for target {} and artifact {}",
                controllerId, sha1Hash);
        if (!controllerManagement.hasTargetArtifactAssigned(controllerId, sha1Hash)) {
            LOG.info("target {} tried to download artifact {} which is not assigned to the target", controllerId,
                    sha1Hash);
            throw new EntityNotFoundException();
        }
        LOG.info("download security check for target {} and artifact {} granted", controllerId, sha1Hash);
    }

    private Optional<Artifact> findArtifactByFileResource(final FileResource fileResource) {

        if (fileResource == null) {
            return Optional.empty();
        }

        if (fileResource.getSha1() != null) {
            return artifactManagement.findFirstBySHA1(fileResource.getSha1());
        }

        if (fileResource.getFilename() != null) {
            return artifactManagement.getByFilename(fileResource.getFilename());
        }

        if (fileResource.getArtifactId() != null) {
            return artifactManagement.get(fileResource.getArtifactId());
        }

        if (fileResource.getSoftwareModuleFilenameResource() != null) {
            return artifactManagement.getByFilenameAndSoftwareModule(
                    fileResource.getSoftwareModuleFilenameResource().getFilename(),
                    fileResource.getSoftwareModuleFilenameResource().getSoftwareModuleId());
        }

        return Optional.empty();
    }

    private static DmfArtifact convertDbArtifact(final Artifact dbArtifact) {
        final DmfArtifact artifact = new DmfArtifact();
        artifact.setSize(dbArtifact.getSize());
        artifact.setLastModified(dbArtifact.getCreatedAt());
        artifact.setHashes(new DmfArtifactHash(dbArtifact.getSha1Hash(), dbArtifact.getMd5Hash()));
        return artifact;
    }

    private Message handleAuthenticationMessage(final Message message) {
        final DmfDownloadResponse authenticationResponse = new DmfDownloadResponse();
        final DmfTenantSecurityToken secruityToken = convertMessage(message, DmfTenantSecurityToken.class);
        final FileResource fileResource = secruityToken.getFileResource();
        try {
            SecurityContextHolder.getContext().setAuthentication(authenticationManager.doAuthenticate(secruityToken));

            final Artifact artifact = findArtifactByFileResource(fileResource)
                    .orElseThrow(EntityNotFoundException::new);

            checkIfArtifactIsAssignedToTarget(secruityToken, artifact.getSha1Hash());

            final DmfArtifact dmfArtifact = convertDbArtifact(artifact);

            authenticationResponse.setArtifact(dmfArtifact);
            final String downloadId = UUID.randomUUID().toString();
            // SHA1 key is set, download by SHA1
            final DownloadArtifactCache downloadCache = new DownloadArtifactCache(DownloadType.BY_SHA1,
                    artifact.getSha1Hash());
            cache.put(downloadId, downloadCache);
            authenticationResponse.setDownloadUrl(UriComponentsBuilder
                    .fromUri(hostnameResolver.resolveHostname().toURI()).path("/api/v1/downloadserver/downloadId/")
                    .path(tenantAware.getCurrentTenant()).path("/").path(downloadId).build().toUriString());
            authenticationResponse.setResponseCode(HttpStatus.OK.value());
        } catch (final BadCredentialsException | AuthenticationServiceException | CredentialsExpiredException e) {
            LOG.error("Login failed", e);
            authenticationResponse.setResponseCode(HttpStatus.FORBIDDEN.value());
            authenticationResponse.setMessage("Login failed");
        } catch (final URISyntaxException e) {
            LOG.error("URI build exception", e);
            authenticationResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            authenticationResponse.setMessage("Building download URI failed");
        } catch (final EntityNotFoundException e) {
            final String errorMessage = "Artifact for resource " + fileResource + "not found ";
            LOG.warn(errorMessage, e);
            authenticationResponse.setResponseCode(HttpStatus.NOT_FOUND.value());
            authenticationResponse.setMessage(errorMessage);
        }

        return getMessageConverter().toMessage(authenticationResponse, message.getMessageProperties());
    }

}
