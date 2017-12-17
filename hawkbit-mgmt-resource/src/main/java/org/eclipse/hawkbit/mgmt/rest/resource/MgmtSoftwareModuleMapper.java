/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifactHash;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtSoftwareModuleMapper {
    private MgmtSoftwareModuleMapper() {
        // Utility class
    }

    private static SoftwareModuleCreate fromRequest(final EntityFactory entityFactory,
            final MgmtSoftwareModuleRequestBodyPost smsRest) {
        return entityFactory.softwareModule().create().type(smsRest.getType()).name(smsRest.getName())
                .version(smsRest.getVersion()).description(smsRest.getDescription()).vendor(smsRest.getVendor());
    }

    static List<SoftwareModuleMetadataCreate> fromRequestSwMetadata(final EntityFactory entityFactory,
            final Long softwareModuleId, final Collection<MgmtSoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream()
                .map(metadataRest -> entityFactory.softwareModuleMetadata().create(softwareModuleId)
                        .key(metadataRest.getKey()).value(metadataRest.getValue())
                        .targetVisible(metadataRest.isTargetVisible()))
                .collect(Collectors.toList());
    }

    static List<SoftwareModuleCreate> smFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtSoftwareModuleRequestBodyPost> smsRest) {
        if (smsRest == null) {
            return Collections.emptyList();
        }

        return smsRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).collect(Collectors.toList());
    }

    static List<MgmtSoftwareModule> toResponse(final Collection<SoftwareModule> softwareModules) {
        if (softwareModules == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                softwareModules.stream().map(MgmtSoftwareModuleMapper::toResponse).collect(Collectors.toList()));
    }

    static List<MgmtSoftwareModuleMetadata> toResponseSwMetadata(
            final Collection<SoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream().map(MgmtSoftwareModuleMapper::toResponseSwMetadata).collect(Collectors.toList());
    }

    static MgmtSoftwareModuleMetadata toResponseSwMetadata(final SoftwareModuleMetadata metadata) {
        final MgmtSoftwareModuleMetadata metadataRest = new MgmtSoftwareModuleMetadata();
        metadataRest.setKey(metadata.getKey());
        metadataRest.setValue(metadata.getValue());
        metadataRest.setTargetVisible(metadata.isTargetVisible());
        return metadataRest;
    }

    static MgmtSoftwareModule toResponse(final SoftwareModule softwareModule) {
        if (softwareModule == null) {
            return null;
        }

        final MgmtSoftwareModule response = new MgmtSoftwareModule();
        MgmtRestModelMapper.mapNamedToNamed(response, softwareModule);
        response.setModuleId(softwareModule.getId());
        response.setVersion(softwareModule.getVersion());
        response.setType(softwareModule.getType().getKey());
        response.setVendor(softwareModule.getVendor());

        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(response.getModuleId()))
                .withSelfRel());

        return response;
    }

    static void addLinks(final SoftwareModule softwareModule, final MgmtSoftwareModule response) {
        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getArtifacts(response.getModuleId()))
                .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_ARTIFACT));

        response.add(linkTo(
                methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(softwareModule.getType().getId()))
                        .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_TYPE));

        response.add(linkTo(methodOn(MgmtSoftwareModuleResource.class).getMetadata(response.getModuleId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("metadata")
                        .expand());
    }

    static MgmtArtifact toResponse(final Artifact artifact) {
        final MgmtArtifact artifactRest = new MgmtArtifact();
        artifactRest.setArtifactId(artifact.getId());
        artifactRest.setSize(artifact.getSize());
        artifactRest.setHashes(new MgmtArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));

        artifactRest.setProvidedFilename(artifact.getFilename());

        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);

        artifactRest.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class)
                .getArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withSelfRel());

        return artifactRest;
    }

    static void addLinks(final Artifact artifact, final MgmtArtifact response) {

        response.add(linkTo(methodOn(MgmtDownloadArtifactResource.class)
                .downloadArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withRel("download"));
    }

    static List<MgmtArtifact> artifactsToResponse(final Collection<Artifact> artifacts) {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                artifacts.stream().map(MgmtSoftwareModuleMapper::toResponse).collect(Collectors.toList()));
    }
}
