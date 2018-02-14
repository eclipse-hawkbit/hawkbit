/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.api.ApiType;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.URLPlaceholder;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifactHash;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfig;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiMetadata;
import org.eclipse.hawkbit.ddi.json.model.DdiPolling;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for the DDI API.
 */
public final class DataConversionHelper {
    // utility class, private constructor.
    private DataConversionHelper() {

    }

    static List<DdiChunk> createChunks(final Target target, final Action uAction,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
            final HttpRequest request, final ControllerManagement controllerManagement) {

        final Map<Long, List<SoftwareModuleMetadata>> metadata = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(uAction.getDistributionSet().getModules().stream()
                        .map(SoftwareModule::getId).collect(Collectors.toList()));

        return uAction.getDistributionSet().getModules().stream()
                .map(module -> new DdiChunk(mapChunkLegacyKeys(module.getType().getKey()), module.getVersion(),
                        module.getName(),
                        createArtifacts(target, module, artifactUrlHandler, systemManagement, request),
                        mapMetadata(metadata.get(module.getId()))))
                .collect(Collectors.toList());

    }

    private static List<DdiMetadata> mapMetadata(final List<SoftwareModuleMetadata> metadata) {
        return CollectionUtils.isEmpty(metadata) ? null
                : metadata.stream().map(md -> new DdiMetadata(md.getKey(), md.getValue())).collect(Collectors.toList());
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

    static List<DdiArtifact> createArtifacts(final Target target, final SoftwareModule module,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
            final HttpRequest request) {

        return new ResponseList<>(module.getArtifacts().stream()
                .map(artifact -> createArtifact(target, artifactUrlHandler, artifact, systemManagement, request))
                .collect(Collectors.toList()));
    }

    private static DdiArtifact createArtifact(final Target target, final ArtifactUrlHandler artifactUrlHandler,
            final Artifact artifact, final SystemManagement systemManagement, final HttpRequest request) {
        final DdiArtifact file = new DdiArtifact();
        file.setHashes(new DdiArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));
        file.setFilename(artifact.getFilename());
        file.setSize(artifact.getSize());

        artifactUrlHandler
                .getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(),
                        systemManagement.getTenantMetadata().getId(), target.getControllerId(), target.getId(),
                        new SoftwareData(artifact.getSoftwareModule().getId(), artifact.getFilename(), artifact.getId(),
                                artifact.getSha1Hash())),
                        ApiType.DDI, request.getURI())
                .forEach(entry -> file.add(new Link(entry.getRef()).withRel(entry.getRel())));

        return file;

    }

    static DdiControllerBase fromTarget(final Target target, final Action action,
            final String defaultControllerPollTime, final TenantAware tenantAware) {
        final DdiControllerBase result = new DdiControllerBase(
                new DdiConfig(new DdiPolling(defaultControllerPollTime)));

        if (action != null) {
            if (action.isCancelingOrCanceled()) {
                result.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(DdiRootController.class, tenantAware.getCurrentTenant())
                                .getControllerCancelAction(tenantAware.getCurrentTenant(), target.getControllerId(),
                                        action.getId()))
                        .withRel(DdiRestConstants.CANCEL_ACTION));
            } else {
                // we need to add the hashcode here of the actionWithStatus
                // because the action might
                // have changed from 'soft' to 'forced' type and we need to
                // change the payload of the
                // response because of eTags.
                result.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(DdiRootController.class, tenantAware.getCurrentTenant())
                                .getControllerBasedeploymentAction(tenantAware.getCurrentTenant(),
                                        target.getControllerId(), action.getId(), calculateEtag(action), null))
                        .withRel(DdiRestConstants.DEPLOYMENT_BASE_ACTION));
            }
        }

        if (target.isRequestControllerAttributes()) {
            result.add(ControllerLinkBuilder
                    .linkTo(ControllerLinkBuilder.methodOn(DdiRootController.class, tenantAware.getCurrentTenant())
                            .putConfigData(null, tenantAware.getCurrentTenant(), target.getControllerId()))
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

}
