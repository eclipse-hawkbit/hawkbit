/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ValidationException;

import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrl;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver;
import org.eclipse.hawkbit.artifact.urlresolver.ArtifactUrlResolver.DownloadDescriptor;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifactHash;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleMetadata;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtDownloadArtifactResource;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleResource;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement.Create;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.json.model.ResponseList;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@Service
public final class MgmtSoftwareModuleMapper {

    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;

    MgmtSoftwareModuleMapper(final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    public static Map<String, MetadataValueCreate> fromRequestSwMetadata(final Collection<MgmtSoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyMap();
        }
        return metadata.stream().collect(Collectors.toMap(
                MgmtSoftwareModuleMetadata::getKey,
                metadataRest -> new MetadataValueCreate(metadataRest.getValue(), metadataRest.isTargetVisible())));
    }

    public List<Create> smFromRequest(final Collection<MgmtSoftwareModuleRequestBodyPost> smsRest) {
        if (smsRest == null) {
            return Collections.emptyList();
        }
        return smsRest.stream().map(this::fromRequest).toList();
    }

    public static List<MgmtSoftwareModule> toResponse(final Collection<? extends SoftwareModule> softwareModules) {
        if (softwareModules == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(softwareModules.stream().map(MgmtSoftwareModuleMapper::toResponse).toList());
    }

    public static List<MgmtSoftwareModuleMetadata> toResponseSwMetadata(final Map<String, ? extends MetadataValue> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }
        return metadata.entrySet().stream().map(e -> toResponseSwMetadata(e.getKey(), e.getValue())).toList();
    }

    public static MgmtSoftwareModuleMetadata toResponseSwMetadata(final String key, final MetadataValue metadata) {
        final MgmtSoftwareModuleMetadata metadataRest = new MgmtSoftwareModuleMetadata();
        metadataRest.setKey(key);
        metadataRest.setValue(metadata.getValue());
        metadataRest.setTargetVisible(metadata.isTargetVisible());
        return metadataRest;
    }

    public static MgmtSoftwareModule toResponse(final SoftwareModule softwareModule) {
        if (softwareModule == null) {
            return null;
        }
        final MgmtSoftwareModule response = new MgmtSoftwareModule();
        MgmtRestModelMapper.mapNamedToNamed(response, softwareModule);
        response.setId(softwareModule.getId());
        response.setVersion(softwareModule.getVersion());
        response.setType(softwareModule.getType().getKey());
        response.setTypeName(softwareModule.getType().getName());
        response.setVendor(softwareModule.getVendor());
        response.setLocked(softwareModule.isLocked());
        response.setDeleted(softwareModule.isDeleted());
        response.setEncrypted(softwareModule.isEncrypted());
        response.setComplete(softwareModule.isComplete());
        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(response.getId())).withSelfRel().expand());
        return response;
    }

    public static void addLinks(final SoftwareModule softwareModule, final MgmtSoftwareModule response) {
        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getArtifacts(response.getId(), null, null))
                .withRel("artifacts").expand());
        response.add(linkTo(methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(softwareModule.getType().getId()))
                .withRel("type").expand());
        response.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtSoftwareModuleResource.class).getMetadata(response.getId()))
                .withRel("metadata").expand());
    }

    public static MgmtArtifact toResponse(final Artifact artifact) {
        final MgmtArtifact artifactRest = new MgmtArtifact();
        artifactRest.setId(artifact.getId());
        artifactRest.setSize(artifact.getSize());
        artifactRest.setHashes(new MgmtArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash(), artifact.getSha256Hash()));
        artifactRest.setProvidedFilename(artifact.getFilename());
        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);
        artifactRest.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class)
                .getArtifact(artifact.getSoftwareModule().getId(), artifact.getId(), null)).withSelfRel().expand());
        return artifactRest;
    }

    public static void addLinks(final Artifact artifact, final MgmtArtifact response) {
        response.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtDownloadArtifactResource.class)
                        .downloadArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withRel("download")
                .expand());
    }

    public static void addLinks(final Artifact artifact, final MgmtArtifact response,
            final ArtifactUrlResolver artifactUrlHandler, final SystemManagement systemManagement) {
        final List<ArtifactUrl> urls = artifactUrlHandler.getUrls(
                new DownloadDescriptor(
                        systemManagement.getTenantMetadata().getTenant(), null,
                        artifact.getSoftwareModule().getId(), artifact.getFilename(), artifact.getSha1Hash()),
                ArtifactUrlResolver.ApiType.MGMT, null);
        urls.forEach(entry -> response.add(Link.of(entry.ref()).withRel(entry.rel()).expand()));
    }

    private Create fromRequest(final MgmtSoftwareModuleRequestBodyPost smsRest) {
        return Create.builder()
                .type(getSoftwareModuleTypeFromKeyString(smsRest.getType()))
                .name(smsRest.getName()).version(smsRest.getVersion()).description(smsRest.getDescription()).vendor(smsRest.getVendor())
                .encrypted(smsRest.isEncrypted())
                .build();
    }

    private SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type) {
        if (type == null) {
            throw new ValidationException("type cannot be null");
        }
        return softwareModuleTypeManagement.findByKey(type.trim())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, type.trim()));
    }
}