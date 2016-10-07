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
import java.util.UUID;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.cache.DownloadArtifactCache;
import org.eclipse.hawkbit.cache.DownloadType;
import org.eclipse.hawkbit.dmf.json.model.Artifact;
import org.eclipse.hawkbit.dmf.json.model.ArtifactHash;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
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

    private final Cache cache;

    private final HostnameResolver hostnameResolver;

    private final ControllerManagement controllerManagement;

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
     */
    public AmqpAuthenticationMessageHandler(final RabbitTemplate rabbitTemplate,
            final AmqpControllerAuthentication authenticationManager, final ArtifactManagement artifactManagement,
            final Cache cache, final HostnameResolver hostnameResolver,
            final ControllerManagement controllerManagement) {
        super(rabbitTemplate);
        this.authenticationManager = authenticationManager;
        this.artifactManagement = artifactManagement;
        this.cache = cache;
        this.hostnameResolver = hostnameResolver;
        this.controllerManagement = controllerManagement;
    }

    /**
     * Executed on a authentication request.
     * 
     * @param message
     *            the amqp message
     * @return the rpc message back to supplier.
     */
    @RabbitListener(queues = "${hawkbit.dmf.rabbitmq.authenticationReceiverQueue}", containerFactory = "listenerContainerFactory")
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
     * @param localArtifact
     *            the local artifact to verify if the given target is allowed to
     *            download this artifact
     */
    private void checkIfArtifactIsAssignedToTarget(final TenantSecurityToken secruityToken,
            final LocalArtifact localArtifact) {

        if (secruityToken.getControllerId() != null) {
            checkByControllerId(localArtifact, secruityToken.getControllerId());
        } else if (secruityToken.getTargetId() != null) {
            checkByTargetId(localArtifact, secruityToken.getTargetId());
        } else {
            LOG.info("anonymous download no authentication check for artifact {}", localArtifact);
            return;
        }

    }

    private void checkByTargetId(final LocalArtifact localArtifact, final Long targetId) {
        LOG.debug("no anonymous download request, doing authentication check for target {} and artifact {}", targetId,
                localArtifact);
        if (!controllerManagement.hasTargetArtifactAssigned(targetId, localArtifact)) {
            LOG.info("target {} tried to download artifact {} which is not assigned to the target", targetId,
                    localArtifact);
            throw new EntityNotFoundException();
        }
        LOG.info("download security check for target {} and artifact {} granted", targetId, localArtifact);
    }

    private void checkByControllerId(final LocalArtifact localArtifact, final String controllerId) {
        LOG.debug("no anonymous download request, doing authentication check for target {} and artifact {}",
                controllerId, localArtifact);
        if (!controllerManagement.hasTargetArtifactAssigned(controllerId, localArtifact)) {
            LOG.info("target {} tried to download artifact {} which is not assigned to the target", controllerId,
                    localArtifact);
            throw new EntityNotFoundException();
        }
        LOG.info("download security check for target {} and artifact {} granted", controllerId, localArtifact);
    }

    private LocalArtifact findLocalArtifactByFileResource(final FileResource fileResource) {
        if (fileResource.getSha1() != null) {
            return artifactManagement.findFirstLocalArtifactsBySHA1(fileResource.getSha1());
        } else if (fileResource.getFilename() != null) {
            return artifactManagement.findLocalArtifactByFilename(fileResource.getFilename()).stream().findFirst()
                    .orElse(null);
        } else if (fileResource.getArtifactId() != null) {
            return artifactManagement.findLocalArtifact(fileResource.getArtifactId());
        } else if (fileResource.getSoftwareModuleFilenameResource() != null) {
            return artifactManagement
                    .findByFilenameAndSoftwareModule(fileResource.getSoftwareModuleFilenameResource().getFilename(),
                            fileResource.getSoftwareModuleFilenameResource().getSoftwareModuleId())
                    .stream().findFirst().orElse(null);
        }
        return null;
    }

    private static Artifact convertDbArtifact(final DbArtifact dbArtifact) {
        final Artifact artifact = new Artifact();
        artifact.setSize(dbArtifact.getSize());
        final DbArtifactHash dbArtifactHash = dbArtifact.getHashes();
        artifact.setHashes(new ArtifactHash(dbArtifactHash.getSha1(), dbArtifactHash.getMd5()));
        return artifact;
    }

    private Message handleAuthenticationMessage(final Message message) {
        final DownloadResponse authentificationResponse = new DownloadResponse();
        final MessageProperties messageProperties = message.getMessageProperties();
        final TenantSecurityToken secruityToken = convertMessage(message, TenantSecurityToken.class);

        final FileResource fileResource = secruityToken.getFileResource();
        try {
            SecurityContextHolder.getContext().setAuthentication(authenticationManager.doAuthenticate(secruityToken));

            final LocalArtifact localArtifact = findLocalArtifactByFileResource(fileResource);

            if (localArtifact == null) {
                LOG.info("target {} requested file resource {} which does not exists to download",
                        secruityToken.getControllerId(), fileResource);
                throw new EntityNotFoundException();
            }

            checkIfArtifactIsAssignedToTarget(secruityToken, localArtifact);

            final Artifact artifact = convertDbArtifact(artifactManagement.loadLocalArtifactBinary(localArtifact));
            if (artifact == null) {
                throw new EntityNotFoundException();
            }
            authentificationResponse.setArtifact(artifact);
            final String downloadId = UUID.randomUUID().toString();
            // SHA1 key is set, download by SHA1
            final DownloadArtifactCache downloadCache = new DownloadArtifactCache(DownloadType.BY_SHA1,
                    localArtifact.getSha1Hash());
            cache.put(downloadId, downloadCache);
            authentificationResponse
                    .setDownloadUrl(UriComponentsBuilder.fromUri(hostnameResolver.resolveHostname().toURI())
                            .path("/api/v1/downloadserver/downloadId/").path(downloadId).build().toUriString());
            authentificationResponse.setResponseCode(HttpStatus.OK.value());
        } catch (final BadCredentialsException | AuthenticationServiceException | CredentialsExpiredException e) {
            LOG.error("Login failed", e);
            authentificationResponse.setResponseCode(HttpStatus.FORBIDDEN.value());
            authentificationResponse.setMessage("Login failed");
        } catch (final URISyntaxException e) {
            LOG.error("URI build exception", e);
            authentificationResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            authentificationResponse.setMessage("Building download URI failed");
        } catch (final EntityNotFoundException e) {
            final String errorMessage = "Artifact for resource " + fileResource + "not found ";
            LOG.warn(errorMessage, e);
            authentificationResponse.setResponseCode(HttpStatus.NOT_FOUND.value());
            authentificationResponse.setMessage(errorMessage);
        }

        return getMessageConverter().toMessage(authentificationResponse, messageProperties);
    }

}
