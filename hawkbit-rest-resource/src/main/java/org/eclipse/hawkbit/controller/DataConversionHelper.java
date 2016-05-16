/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.UrlProtocol;
import org.eclipse.hawkbit.controller.model.Artifact;
import org.eclipse.hawkbit.controller.model.Chunk;
import org.eclipse.hawkbit.controller.model.Config;
import org.eclipse.hawkbit.controller.model.ControllerBase;
import org.eclipse.hawkbit.controller.model.Polling;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactHash;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.hateoas.Link;

import com.google.common.base.Charsets;

/**
 * Utility class for the Controller API.
 */
public final class DataConversionHelper {
    // utility class, private constructor.
    private DataConversionHelper() {

    }

    static List<Chunk> createChunks(final String targetid, final Action uAction,
            final ArtifactUrlHandler artifactUrlHandler) {
        return uAction.getDistributionSet().getModules().stream()
                .map(module -> new Chunk(mapChunkLegacyKeys(module.getType().getKey()), module.getVersion(),
                        module.getName(), createArtifacts(targetid, module, artifactUrlHandler)))
                .collect(Collectors.toList());
    }

    private static String mapChunkLegacyKeys(final String key) {
        if ("application".equals(key)) {
            return "bApp";
        }
        if ("runtime".equals(key)) {
            return "jvm";
        }

        return key;
    }

    /**
     * Creates all (rest) artifacts for a given software module.
     *
     * @param targetid
     *            of the target
     * @param module
     *            the software module
     * 
     * @return a list of artifacts or a empty list. Cannot be <null>.
     */
    public static List<Artifact> createArtifacts(final String targetid,
            final org.eclipse.hawkbit.repository.model.SoftwareModule module,
            final ArtifactUrlHandler artifactUrlHandler) {
        final List<Artifact> files = new ArrayList<>();
        module.getLocalArtifacts()
                .forEach(artifact -> files.add(createArtifact(targetid, artifactUrlHandler, artifact)));
        return files;
    }

    private static Artifact createArtifact(final String targetid, final ArtifactUrlHandler artifactUrlHandler,
            final LocalArtifact artifact) {
        final Artifact file = new Artifact();
        file.setHashes(new ArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));
        file.setFilename(artifact.getFilename());
        file.setSize(artifact.getSize());

        if (artifactUrlHandler.protocolSupported(UrlProtocol.HTTP)) {
            final String linkHttp = artifactUrlHandler.getUrl(targetid, artifact.getSoftwareModule().getId(),
                    artifact.getFilename(), artifact.getSha1Hash(), UrlProtocol.HTTP);
            file.add(new Link(linkHttp).withRel("download-http"));
            file.add(new Link(linkHttp + ControllerConstants.ARTIFACT_MD5_DWNL_SUFFIX).withRel("md5sum-http"));
        }

        if (artifactUrlHandler.protocolSupported(UrlProtocol.HTTPS)) {
            final String linkHttps = artifactUrlHandler.getUrl(targetid, artifact.getSoftwareModule().getId(),
                    artifact.getFilename(), artifact.getSha1Hash(), UrlProtocol.HTTPS);
            file.add(new Link(linkHttps).withRel("download"));
            file.add(new Link(linkHttps + ControllerConstants.ARTIFACT_MD5_DWNL_SUFFIX).withRel("md5sum"));
        }
        return file;
    }

    static ControllerBase fromTarget(final Target target, final List<Action> actions,
            final String defaultControllerPollTime, final TenantAware tenantAware) {
        final ControllerBase result = new ControllerBase(new Config(new Polling(defaultControllerPollTime)));

        boolean addedUpdate = false;
        boolean addedCancel = false;
        final long countCancelingActions = actions.stream().filter(a -> a.getStatus() == Status.CANCELING).count();
        for (final Action action : actions) {
            if (countCancelingActions <= 0 && !action.isCancelingOrCanceled() && !addedUpdate) {
                // we need to add the hashcode here of the actionWithStatus
                // because the action might
                // have changed from 'soft' to 'forced' type and we need to
                // change the payload of the
                // response because of eTags.
                result.add(linkTo(methodOn(RootController.class, tenantAware.getCurrentTenant())
                        .getControllerBasedeploymentAction(target.getControllerId(), action.getId(), actions.hashCode(),
                                null)).withRel(ControllerConstants.DEPLOYMENT_BASE_ACTION));
                addedUpdate = true;
            } else if (action.isCancelingOrCanceled() && !addedCancel) {
                result.add(linkTo(methodOn(RootController.class, tenantAware.getCurrentTenant())
                        .getControllerCancelAction(target.getControllerId(), action.getId(), null))
                                .withRel(ControllerConstants.CANCEL_ACTION));
                addedCancel = true;
            }
        }

        if (target.getTargetInfo().isRequestControllerAttributes()) {
            result.add(linkTo(methodOn(RootController.class, tenantAware.getCurrentTenant()).putConfigData(null,
                    target.getControllerId(), null)).withRel(ControllerConstants.CONFIG_DATA_ACTION));
        }
        return result;
    }

    static void writeMD5FileResponse(final String fileName, final HttpServletResponse response,
            final LocalArtifact artifact) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append(artifact.getMd5Hash());
        builder.append("  ");
        builder.append(fileName);
        final byte[] content = builder.toString().getBytes(Charsets.US_ASCII);

        final StringBuilder header = new StringBuilder();
        header.append("attachment;filename=");
        header.append(fileName);
        header.append(ControllerConstants.ARTIFACT_MD5_DWNL_SUFFIX);

        response.setContentLength(content.length);
        response.setHeader("Content-Disposition", header.toString());

        response.getOutputStream().write(content, 0, content.length);
    }

}
