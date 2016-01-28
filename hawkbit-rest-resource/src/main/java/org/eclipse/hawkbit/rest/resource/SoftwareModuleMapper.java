/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactHash;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactRest;
import org.eclipse.hawkbit.rest.resource.model.artifact.ArtifactsRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModuleRest;
import org.eclipse.hawkbit.rest.resource.model.softwaremodule.SoftwareModulesRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 *
 *
 */
public final class SoftwareModuleMapper {
    private SoftwareModuleMapper() {
        // Utility class
    }

    private static SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type,
            final SoftwareManagement softwareManagement) {

        final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeByKey(type.trim());

        if (smType == null) {
            throw new EntityNotFoundException(type.trim());
        }

        return smType;
    }

    static SoftwareModule fromRequest(final SoftwareModuleRequestBodyPost smsRest,
            final SoftwareManagement softwareManagement) {
        return new SoftwareModule(getSoftwareModuleTypeFromKeyString(smsRest.getType(), softwareManagement),
                smsRest.getName(), smsRest.getVersion(), smsRest.getDescription(), smsRest.getVendor());
    }

    static List<SoftwareModuleMetadata> fromRequestSwMetadata(final SoftwareModule sw,
            final List<MetadataRest> metadata) {
        final List<SoftwareModuleMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final MetadataRest metadataRest : metadata) {
            if (metadataRest.getKey() == null) {
                throw new IllegalArgumentException("the key of the metadata must be present");
            }
            mappedList.add(new SoftwareModuleMetadata(metadataRest.getKey(), sw, metadataRest.getValue()));
        }
        return mappedList;
    }

    static List<SoftwareModule> smFromRequest(final Iterable<SoftwareModuleRequestBodyPost> smsRest,
            final SoftwareManagement softwareManagement) {
        final List<SoftwareModule> mappedList = new ArrayList<>();
        for (final SoftwareModuleRequestBodyPost smRest : smsRest) {
            mappedList.add(fromRequest(smRest, softwareManagement));
        }
        return mappedList;
    }

    /**
     * Create response for sw modules.
     * 
     * @param baseSoftareModules
     *            the modules
     * @return the response
     */
    public static List<SoftwareModuleRest> toResponse(final List<SoftwareModule> baseSoftareModules) {
        final List<SoftwareModuleRest> mappedList = new ArrayList<>();
        if (baseSoftareModules != null) {
            for (final SoftwareModule target : baseSoftareModules) {
                final SoftwareModuleRest response = toResponse(target);

                mappedList.add(response);
            }
        }
        return mappedList;
    }

    static SoftwareModulesRest toResponseSoftwareModules(final Iterable<SoftwareModule> softwareModules) {
        final SoftwareModulesRest response = new SoftwareModulesRest();
        for (final SoftwareModule softwareModule : softwareModules) {
            response.add(toResponse(softwareModule));
        }
        return response;
    }

    static List<MetadataRest> toResponseSwMetadata(final List<SoftwareModuleMetadata> metadata) {
        final List<MetadataRest> mappedList = new ArrayList<>(metadata.size());
        for (final SoftwareModuleMetadata distributionSetMetadata : metadata) {
            mappedList.add(toResponseSwMetadata(distributionSetMetadata));
        }
        return mappedList;
    }

    static MetadataRest toResponseSwMetadata(final SoftwareModuleMetadata metadata) {
        final MetadataRest metadataRest = new MetadataRest();
        metadataRest.setKey(metadata.getId().getKey());
        metadataRest.setValue(metadata.getValue());
        return metadataRest;
    }

    /**
     * Create response for one sw module.
     * 
     * @param baseSofwareModule
     *            the sw module
     * @return the response
     */
    public static SoftwareModuleRest toResponse(final SoftwareModule baseSofwareModule) {
        if (baseSofwareModule == null) {
            return null;
        }

        final SoftwareModuleRest response = new SoftwareModuleRest();
        RestModelMapper.mapNamedToNamed(response, baseSofwareModule);
        response.setModuleId(baseSofwareModule.getId());
        response.setVersion(baseSofwareModule.getVersion());
        response.setType(baseSofwareModule.getType().getKey());
        response.setVendor(baseSofwareModule.getVendor());

        response.add(linkTo(methodOn(SoftwareModuleResource.class).getArtifacts(response.getModuleId()))
                .withRel(RestConstants.SOFTWAREMODULE_V1_ARTIFACT));
        response.add(linkTo(methodOn(SoftwareModuleResource.class).getSoftwareModule(response.getModuleId()))
                .withRel("self"));

        response.add(linkTo(
                methodOn(SoftwareModuleTypeResource.class).getSoftwareModuleType(baseSofwareModule.getType().getId()))
                        .withRel(RestConstants.SOFTWAREMODULE_V1_TYPE));

        response.add(linkTo(methodOn(SoftwareModuleResource.class).getMetadata(response.getModuleId(),
                Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));
        return response;
    }

    /**
     * @param artifact
     * @return
     */
    static ArtifactRest toResponse(final Artifact artifact) {
        final ArtifactRest.ArtifactType type = artifact instanceof LocalArtifact ? ArtifactRest.ArtifactType.LOCAL
                : ArtifactRest.ArtifactType.EXTERNAL;

        final ArtifactRest artifactRest = new ArtifactRest();
        artifactRest.setType(type);
        artifactRest.setArtifactId(artifact.getId());
        artifactRest.setSize(artifact.getSize());
        artifactRest.setHashes(new ArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));

        if (artifact instanceof LocalArtifact) {
            artifactRest.setProvidedFilename(((LocalArtifact) artifact).getFilename());
        }

        RestModelMapper.mapBaseToBase(artifactRest, artifact);

        artifactRest.add(linkTo(methodOn(SoftwareModuleResource.class).getArtifact(artifact.getSoftwareModule().getId(),
                artifact.getId())).withRel("self"));

        if (artifact instanceof LocalArtifact) {
            artifactRest.add(
                    linkTo(methodOn(SoftwareModuleResource.class).downloadArtifact(artifact.getSoftwareModule().getId(),
                            artifact.getId(), null, null)).withRel("download"));
        }

        return artifactRest;
    }

    static ArtifactsRest artifactsToResponse(final List<Artifact> artifacts) {
        final ArtifactsRest mappedList = new ArtifactsRest();

        if (artifacts != null) {
            for (final Artifact artifact : artifacts) {
                final ArtifactRest response = toResponse(artifact);
                mappedList.add(response);
            }
        }
        return mappedList;
    }
}
