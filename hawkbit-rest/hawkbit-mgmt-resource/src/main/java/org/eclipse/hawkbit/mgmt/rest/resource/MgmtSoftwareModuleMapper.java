/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.api.ApiType;
import org.eclipse.hawkbit.api.ArtifactUrl;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.URLPlaceholder;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifactHash;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.springframework.hateoas.Link;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtSoftwareModuleMapper {

    private static SoftwareModuleCreate fromRequest(final EntityFactory entityFactory,
            final MgmtSoftwareModuleRequestBodyPost smsRest) {
        return entityFactory.softwareModule().create().type(smsRest.getType()).name(smsRest.getName())
                .version(smsRest.getVersion()).description(smsRest.getDescription()).vendor(smsRest.getVendor())
                .encrypted(smsRest.isEncrypted());
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

    static List<MgmtSoftwareModuleMetadata> toResponseSwMetadata(final Collection<SoftwareModuleMetadata> metadata) {
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
        response.setTypeName(softwareModule.getType().getName());
        response.setVendor(softwareModule.getVendor());
        response.setLocked(softwareModule.isLocked());
        response.setDeleted(softwareModule.isDeleted());
        response.setEncrypted(softwareModule.isEncrypted());

        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(response.getModuleId()))
                .withSelfRel().expand());

        return response;
    }

    static void addLinks(final SoftwareModule softwareModule, final MgmtSoftwareModule response) {
        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getArtifacts(response.getModuleId(), null, null))
                .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_ARTIFACT).expand());

        response.add(linkTo(
                methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(softwareModule.getType().getId()))
                        .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_TYPE).expand());

        response.add(linkTo(methodOn(MgmtSoftwareModuleResource.class).getMetadata(response.getModuleId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("metadata")
                        .expand().expand());
    }

    static MgmtArtifact toResponse(final Artifact artifact) {
        final MgmtArtifact artifactRest = new MgmtArtifact();
        artifactRest.setArtifactId(artifact.getId());
        artifactRest.setSize(artifact.getSize());
        artifactRest.setHashes(
                new MgmtArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash(), artifact.getSha256Hash()));

        artifactRest.setProvidedFilename(artifact.getFilename());

        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);

        artifactRest.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class)
                .getArtifact(artifact.getSoftwareModule().getId(), artifact.getId(), null)).withSelfRel().expand());

        return artifactRest;
    }

    static void addLinks(final Artifact artifact, final MgmtArtifact response) {
        response.add(linkTo(methodOn(MgmtDownloadArtifactResource.class)
                .downloadArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withRel("download")
                        .expand());
    }

    static void addLinks(final Artifact artifact, final MgmtArtifact response,
            final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement) {
        final List<ArtifactUrl> urls = artifactUrlHandler.getUrls(
                new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(),
                        systemManagement.getTenantMetadata().getId(), null, null,
                        new URLPlaceholder.SoftwareData(artifact.getSoftwareModule().getId(), artifact.getFilename(),
                                artifact.getId(), artifact.getSha1Hash())), ApiType.MGMT, null);
        urls.forEach(entry -> response.add(Link.of(entry.getRef()).withRel(entry.getRel()).expand()));
    }
}