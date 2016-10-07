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

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifactHash;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.ConstraintViolationException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtSoftwareModuleMapper {
    private MgmtSoftwareModuleMapper() {
        // Utility class
    }

    private static SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type,
            final SoftwareManagement softwareManagement) {
        if (type == null) {
            throw new ConstraintViolationException("type cannot be null");
        }

        final SoftwareModuleType smType = softwareManagement.findSoftwareModuleTypeByKey(type.trim());

        if (smType == null) {
            throw new EntityNotFoundException(type.trim());
        }

        return smType;
    }

    static SoftwareModule fromRequest(final EntityFactory entityFactory,
            final MgmtSoftwareModuleRequestBodyPost smsRest, final SoftwareManagement softwareManagement) {
        return entityFactory.generateSoftwareModule(
                getSoftwareModuleTypeFromKeyString(smsRest.getType(), softwareManagement), smsRest.getName(),
                smsRest.getVersion(), smsRest.getDescription(), smsRest.getVendor());
    }

    static List<SoftwareModuleMetadata> fromRequestSwMetadata(final EntityFactory entityFactory,
            final SoftwareModule sw, final Collection<MgmtMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream().map(metadataRest -> entityFactory.generateSoftwareModuleMetadata(sw,
                metadataRest.getKey(), metadataRest.getValue())).collect(Collectors.toList());
    }

    static List<SoftwareModule> smFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtSoftwareModuleRequestBodyPost> smsRest, final SoftwareManagement softwareManagement) {
        if (smsRest == null) {
            return Collections.emptyList();
        }

        return smsRest.stream().map(smRest -> fromRequest(entityFactory, smRest, softwareManagement))
                .collect(Collectors.toList());
    }

    /**
     * Create response for sw modules.
     * 
     * @param softwareModules
     *            the modules
     * @return the response
     */
    public static List<MgmtSoftwareModule> toResponse(final Collection<SoftwareModule> softwareModules) {
        if (softwareModules == null) {
            return Collections.emptyList();
        }

        return softwareModules.stream().map(MgmtSoftwareModuleMapper::toResponse).collect(Collectors.toList());
    }

    static List<MgmtMetadata> toResponseSwMetadata(final Collection<SoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream().map(MgmtSoftwareModuleMapper::toResponseSwMetadata).collect(Collectors.toList());
    }

    static MgmtMetadata toResponseSwMetadata(final SoftwareModuleMetadata metadata) {
        final MgmtMetadata metadataRest = new MgmtMetadata();
        metadataRest.setKey(metadata.getKey());
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
    public static MgmtSoftwareModule toResponse(final SoftwareModule baseSofwareModule) {
        if (baseSofwareModule == null) {
            return null;
        }

        final MgmtSoftwareModule response = new MgmtSoftwareModule();
        MgmtRestModelMapper.mapNamedToNamed(response, baseSofwareModule);
        response.setModuleId(baseSofwareModule.getId());
        response.setVersion(baseSofwareModule.getVersion());
        response.setType(baseSofwareModule.getType().getKey());
        response.setVendor(baseSofwareModule.getVendor());

        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getArtifacts(response.getModuleId()))
                .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_ARTIFACT));
        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(response.getModuleId()))
                .withRel("self"));

        response.add(linkTo(methodOn(MgmtSoftwareModuleTypeRestApi.class)
                .getSoftwareModuleType(baseSofwareModule.getType().getId()))
                        .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_TYPE));

        response.add(linkTo(methodOn(MgmtSoftwareModuleResource.class).getMetadata(response.getModuleId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));
        return response;
    }

    /**
     * @param artifact
     * @return
     */
    static MgmtArtifact toResponse(final Artifact artifact) {
        final MgmtArtifact.ArtifactType type = artifact instanceof LocalArtifact ? MgmtArtifact.ArtifactType.LOCAL
                : MgmtArtifact.ArtifactType.EXTERNAL;

        final MgmtArtifact artifactRest = new MgmtArtifact();
        artifactRest.setType(type);
        artifactRest.setArtifactId(artifact.getId());
        artifactRest.setSize(artifact.getSize());
        artifactRest.setHashes(new MgmtArtifactHash(artifact.getSha1Hash(), artifact.getMd5Hash()));

        if (artifact instanceof LocalArtifact) {
            artifactRest.setProvidedFilename(((LocalArtifact) artifact).getFilename());
        }

        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);

        artifactRest.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class)
                .getArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withRel("self"));

        if (artifact instanceof LocalArtifact) {
            artifactRest.add(linkTo(methodOn(MgmtDownloadArtifactResource.class)
                    .downloadArtifact(artifact.getSoftwareModule().getId(), artifact.getId())).withRel("download"));
        }

        return artifactRest;
    }

    static List<MgmtArtifact> artifactsToResponse(final Collection<Artifact> artifacts) {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return artifacts.stream().map(MgmtSoftwareModuleMapper::toResponse).collect(Collectors.toList());
    }
}
