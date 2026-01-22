/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver.DownloadDescriptor;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifactHash;
import org.eclipse.hawkbit.ddi.json.model.DdiAutoConfirmationState;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfig;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationBase;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiMetadata;
import org.eclipse.hawkbit.ddi.json.model.DdiPolling;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for the DDI API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConversionHelper {

    public static DdiConfirmationBase createConfirmationBase(
            final Target target, final Action activeAction, final DdiAutoConfirmationState autoConfirmationState) {
        final String controllerId = target.getControllerId();
        final DdiConfirmationBase confirmationBase = new DdiConfirmationBase(autoConfirmationState);
        if (autoConfirmationState.isActive()) {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class, AccessContext.tenant())
                            .deactivateAutoConfirmation(AccessContext.tenant(), controllerId))
                    .withRel(DdiRootControllerRestApi.DEACTIVATE_AUTO_CONFIRM).expand());
        } else {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class, AccessContext.tenant())
                            .activateAutoConfirmation(AccessContext.tenant(), controllerId, null))
                    .withRel(DdiRootControllerRestApi.ACTIVATE_AUTO_CONFIRM).expand());
        }
        if (activeAction != null && activeAction.isWaitingConfirmation()) {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class, AccessContext.tenant())
                            .getConfirmationBaseAction(AccessContext.tenant(), controllerId,
                                    activeAction.getId(), calculateEtag(activeAction), null))
                    .withRel(DdiRootControllerRestApi.CONFIRMATION_BASE).expand());
        }

        return confirmationBase;
    }

    public static DdiControllerBase fromTarget(
            final Target target, final Action installedAction,
            final Action activeAction, final String defaultControllerPollTime) {
        final DdiControllerBase result = new DdiControllerBase(
                new DdiConfig(new DdiPolling(defaultControllerPollTime)));

        if (activeAction != null) {
            if (activeAction.isWaitingConfirmation()) {
                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder
                                .methodOn(DdiRootController.class, AccessContext.tenant())
                                .getConfirmationBaseAction(AccessContext.tenant(), target.getControllerId(),
                                        activeAction.getId(), calculateEtag(activeAction), null))
                        .withRel(DdiRootControllerRestApi.CONFIRMATION_BASE).expand());

            } else if (activeAction.isCancelingOrCanceled()) {
                result.add(WebMvcLinkBuilder
                        .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class, AccessContext.tenant())
                                .getControllerCancelAction(AccessContext.tenant(), target.getControllerId(),
                                        activeAction.getId()))
                        .withRel(DdiRootControllerRestApi.CANCEL_ACTION).expand());
            } else {
                // we need to add the hashcode here of the actionWithStatus because the action might
                // have changed from 'soft' to 'forced' type, and we need to change the payload of the
                // response because of eTags.
                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder
                                .methodOn(DdiRootController.class, AccessContext.tenant())
                                .getControllerDeploymentBaseAction(
                                        AccessContext.tenant(), target.getControllerId(),
                                        activeAction.getId(), calculateEtag(activeAction), null))
                        .withRel(DdiRootControllerRestApi.DEPLOYMENT_BASE).expand());
            }
        }

        if (installedAction != null && !installedAction.isActive()) {
            result.add(
                    WebMvcLinkBuilder
                            .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class, AccessContext.tenant())
                                    .getControllerInstalledAction(AccessContext.tenant(),
                                            target.getControllerId(), installedAction.getId(), null))
                            .withRel(DdiRootControllerRestApi.INSTALLED_BASE).expand());
        }

        if (target.isRequestControllerAttributes()) {
            result.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder
                            .methodOn(DdiRootController.class, AccessContext.tenant())
                            // doesn't really call the putConfigData with null, just create the link
                            .putConfigData(null, AccessContext.tenant(), target.getControllerId()))
                    .withRel(DdiRootControllerRestApi.CONFIG_DATA).expand());
        }

        return result;
    }

    static List<DdiChunk> createChunks(
            final Target target, final Action uAction,
            final ArtifactUrlResolver artifactUrlHandler, final SystemManagement systemManagement,
            final HttpRequest request, final ControllerManagement controllerManagement) {
        final Map<Long, Map<String, String>> metadata = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(uAction.getDistributionSet().getModules().stream()
                        .map(SoftwareModule::getId).toList());

        return new ResponseList<>(uAction.getDistributionSet().getModules().stream()
                .map(module -> new DdiChunk(mapChunkLegacyKeys(module.getType().getKey()), module.getVersion(),
                        module.getName(), module.isEncrypted() ? Boolean.TRUE : null,
                        createArtifacts(target, module, artifactUrlHandler, systemManagement, request),
                        mapMetadata(metadata.get(module.getId()))))
                .toList());

    }

    static List<DdiArtifact> createArtifacts(final Target target, final SoftwareModule module,
            final ArtifactUrlResolver artifactUrlHandler, final SystemManagement systemManagement,
            final HttpRequest request) {

        return new ResponseList<>(module.getArtifacts().stream()
                .map(artifact -> createArtifact(target, artifactUrlHandler, artifact, systemManagement, request))
                .toList());
    }

    private static List<DdiMetadata> mapMetadata(final Map<String, String> metadata) {
        return CollectionUtils.isEmpty(metadata)
                ? null
                : metadata.entrySet().stream().map(md -> new DdiMetadata(md.getKey(), md.getValue())).toList();
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

    private static DdiArtifact createArtifact(
            final Target target, final ArtifactUrlResolver artifactUrlHandler,
            final Artifact artifact, final SystemManagement systemManagement, final HttpRequest request) {
        final DdiArtifact file = new DdiArtifact(
                artifact.getFilename(),
                new DdiArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash(), artifact.getSha256Hash()),
                artifact.getSize());

        final TenantMetaData tenantMetadata = systemManagement.getTenantMetadataWithoutDetails();
        artifactUrlHandler
                .getUrls(new DownloadDescriptor(
                                tenantMetadata.getTenant(), target.getControllerId(),
                                artifact.getSoftwareModule().getId(), artifact.getFilename(), artifact.getSha1Hash()),
                        ArtifactUrlResolver.ApiType.DDI, request.getURI())
                .forEach(entry -> file.add(Link.of(entry.ref()).withRel(entry.rel()).expand()));

        return file;
    }

    /**
     * Calculates an etag for the given {@link Action} based on the entities hashcode and the {@link Action#isHitAutoForceTime(long)}
     * to reflect a force switch.
     *
     * @param action to calculate the etag for
     * @return the etag
     */
    private static int calculateEtag(final Action action) {
        final int prime = 31;
        int result = action.hashCode();
        int offsetPrime = action.isHitAutoForceTime(System.currentTimeMillis()) ? 1231 : 1237;
        offsetPrime = action.hasMaintenanceSchedule() && action.isMaintenanceWindowAvailable() ? 1249 : offsetPrime;

        result = prime * result + offsetPrime;
        return result;
    }
}