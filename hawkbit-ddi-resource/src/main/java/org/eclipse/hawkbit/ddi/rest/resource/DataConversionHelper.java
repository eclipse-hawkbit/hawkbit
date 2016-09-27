/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.api.ApiType;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.URLPlaceholder;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.eclipse.hawkbit.ddi.dl.rest.api.DdiDlRestConstants;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifactHash;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfig;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiPolling;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.hateoas.Link;

import com.google.common.base.Charsets;

/**
 * Utility class for the DDI API.
 */
public final class DataConversionHelper {
    // utility class, private constructor.
    private DataConversionHelper() {

    }

    static List<DdiChunk> createChunks(final Target target, final Action uAction,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement) {
        return uAction.getDistributionSet().getModules().stream()
                .map(module -> new DdiChunk(mapChunkLegacyKeys(module.getType().getKey()), module.getVersion(),
                        module.getName(), createArtifacts(target, module, artifactUrlHandler, systemManagement)))
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
     * @param target
     *            to convert
     * @param module
     *            the software module
     * @param artifactUrlHandler
     *            for generating download URLs
     * @param systemManagement
     *            for accessing the tenant ID
     * @return a list of artifacts or a empty list. Cannot be <null>.
     */
    public static List<DdiArtifact> createArtifacts(final Target target,
            final org.eclipse.hawkbit.repository.model.SoftwareModule module,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement) {
        final List<DdiArtifact> files = new ArrayList<>();

        module.getLocalArtifacts()
                .forEach(artifact -> files.add(createArtifact(target, artifactUrlHandler, artifact, systemManagement)));
        return files;
    }

    private static DdiArtifact createArtifact(final Target target, final ArtifactUrlHandler artifactUrlHandler,
            final LocalArtifact artifact, final SystemManagement systemManagement) {
        final DdiArtifact file = new DdiArtifact();
        file.setHashes(new DdiArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));
        file.setFilename(artifact.getFilename());
        file.setSize(artifact.getSize());

        artifactUrlHandler
                .getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(),
                        systemManagement.getTenantMetadata().getId(), target.getControllerId(), target.getId(),
                        new SoftwareData(artifact.getSoftwareModule().getId(), artifact.getFilename(), artifact.getId(),
                                artifact.getSha1Hash())),
                        ApiType.DDI)
                .forEach(entry -> file.add(new Link(entry.getRef()).withRel(entry.getRel())));

        return file;

    }

    static DdiControllerBase fromTarget(final Target target, final Optional<Action> action,
            final String defaultControllerPollTime, final TenantAware tenantAware) {
        final DdiControllerBase result = new DdiControllerBase(
                new DdiConfig(new DdiPolling(defaultControllerPollTime)));

        if (action.isPresent()) {
            if (action.get().isCancelingOrCanceled()) {
                result.add(linkTo(
                        methodOn(DdiRootController.class, tenantAware.getCurrentTenant()).getControllerCancelAction(
                                tenantAware.getCurrentTenant(), target.getControllerId(), action.get().getId()))
                                        .withRel(DdiRestConstants.CANCEL_ACTION));
            } else {
                // we need to add the hashcode here of the actionWithStatus
                // because the action might
                // have changed from 'soft' to 'forced' type and we need to
                // change the payload of the
                // response because of eTags.
                result.add(linkTo(methodOn(DdiRootController.class, tenantAware.getCurrentTenant())
                        .getControllerBasedeploymentAction(tenantAware.getCurrentTenant(), target.getControllerId(),
                                action.get().getId(), calculateEtag(action.get())))
                                        .withRel(DdiRestConstants.DEPLOYMENT_BASE_ACTION));
            }
        }

        if (target.getTargetInfo().isRequestControllerAttributes()) {
            result.add(linkTo(methodOn(DdiRootController.class, tenantAware.getCurrentTenant()).putConfigData(null,
                    tenantAware.getCurrentTenant(), target.getControllerId()))
                            .withRel(DdiRestConstants.CONFIG_DATA_ACTION));
        }
        return result;
    }

    /**
     * Calculates an etag for the given {@link Action} based on the entities
     * hashcode and the {@link Action#isHitAutoForceTime(long)} to reflect a
     * force switch.
     * 
     * @param action
     *            to calculate the etag for
     * @return the etag
     */
    private static int calculateEtag(final Action action) {
        final int prime = 31;
        int result = action.hashCode();
        result = prime * result + (action.isHitAutoForceTime(System.currentTimeMillis()) ? 1231 : 1237);
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
        header.append(DdiDlRestConstants.ARTIFACT_MD5_DWNL_SUFFIX);

        response.setContentLength(content.length);
        response.setHeader("Content-Disposition", header.toString());

        response.getOutputStream().write(content, 0, content.length);
    }

}
