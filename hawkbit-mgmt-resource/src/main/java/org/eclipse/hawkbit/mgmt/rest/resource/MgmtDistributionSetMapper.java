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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtDistributionSetMapper {
    private MgmtDistributionSetMapper() {
        // Utility class
    }

    private static SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId,
            final SoftwareManagement softwareManagement) {
        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (module == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        }

        return module;
    }

    private static DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(
            final String distributionSetTypekey, final DistributionSetManagement distributionSetManagement) {

        final DistributionSetType module = distributionSetManagement
                .findDistributionSetTypeByKey(distributionSetTypekey);
        if (module == null) {
            throw new EntityNotFoundException(
                    "DistributionSetType with key {" + distributionSetTypekey + "} does not exist");
        }
        return module;
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost}s to {@link DistributionSet}s.
     *
     * @param sets
     *            to convert
     * @param softwareManagement
     *            to use for conversion
     * @return converted list of {@link DistributionSet}s
     */
    static List<DistributionSet> dsFromRequest(final Collection<MgmtDistributionSetRequestBodyPost> sets,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final EntityFactory entityFactory) {

        return sets.stream()
                .map(dsRest -> fromRequest(dsRest, softwareManagement, distributionSetManagement, entityFactory))
                .collect(Collectors.toList());

    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost} to {@link DistributionSet}.
     *
     * @param dsRest
     *            to convert
     * @param softwareManagement
     *            to use for conversion
     * @return converted {@link DistributionSet}
     */
    static DistributionSet fromRequest(final MgmtDistributionSetRequestBodyPost dsRest,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement,
            final EntityFactory entityFactory) {

        final List<SoftwareModule> modules = new ArrayList<>(dsRest.getModules().size());

        if (dsRest.getOs() != null) {
            modules.add(findSoftwareModuleWithExceptionIfNotFound(dsRest.getOs().getId(), softwareManagement));
        }

        if (dsRest.getApplication() != null) {
            modules.add(findSoftwareModuleWithExceptionIfNotFound(dsRest.getApplication().getId(), softwareManagement));
        }

        if (dsRest.getRuntime() != null) {
            modules.add(findSoftwareModuleWithExceptionIfNotFound(dsRest.getRuntime().getId(), softwareManagement));
        }

        if (dsRest.getModules() != null) {
            dsRest.getModules().forEach(module -> modules
                    .add(findSoftwareModuleWithExceptionIfNotFound(module.getId(), softwareManagement)));
        }

        return entityFactory.generateDistributionSet(dsRest.getName(), dsRest.getVersion(), dsRest.getDescription(),
                findDistributionSetTypeWithExceptionIfNotFound(dsRest.getType(), distributionSetManagement), modules,
                dsRest.isRequiredMigrationStep());
    }

    static List<MetaData> fromRequestDsMetadata(final List<MgmtMetadata> metadata, final EntityFactory entityFactory) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream()
                .map(metadataRest -> entityFactory.generateMetadata(metadataRest.getKey(), metadataRest.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Create a response for distribution set.
     * 
     * @param distributionSet
     *            the ds set
     * @return the response
     */
    public static MgmtDistributionSet toResponse(final DistributionSet distributionSet) {
        if (distributionSet == null) {
            return null;
        }
        final MgmtDistributionSet response = new MgmtDistributionSet();
        MgmtRestModelMapper.mapNamedToNamed(response, distributionSet);

        response.setDsId(distributionSet.getId());
        response.setVersion(distributionSet.getVersion());
        response.setComplete(distributionSet.isComplete());
        response.setType(distributionSet.getType().getKey());

        distributionSet.getModules()
                .forEach(module -> response.getModules().add(MgmtSoftwareModuleMapper.toResponse(module)));

        response.setRequiredMigrationStep(distributionSet.isRequiredMigrationStep());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(response.getDsId()))
                .withRel("self"));

        response.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(distributionSet.getType().getId())).withRel("type"));

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(response.getDsId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));

        return response;
    }

    static MgmtTargetAssignmentResponseBody toResponse(final DistributionSetAssignmentResult dsAssignmentResult) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        result.setAssigned(dsAssignmentResult.getAssigned());
        result.setAlreadyAssigned(dsAssignmentResult.getAlreadyAssigned());
        result.setTotal(dsAssignmentResult.getTotal());
        return result;
    }

    static List<MgmtDistributionSet> toResponseDistributionSets(final Collection<DistributionSet> sets) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return sets.stream().map(MgmtDistributionSetMapper::toResponse).collect(Collectors.toList());
    }

    static MgmtMetadata toResponseDsMetadata(final DistributionSetMetadata metadata) {
        final MgmtMetadata metadataRest = new MgmtMetadata();
        metadataRest.setKey(metadata.getKey());
        metadataRest.setValue(metadata.getValue());
        return metadataRest;
    }

    static List<MgmtMetadata> toResponseDsMetadata(final List<DistributionSetMetadata> metadata) {

        final List<MgmtMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final DistributionSetMetadata distributionSetMetadata : metadata) {
            mappedList.add(toResponseDsMetadata(distributionSetMetadata));
        }
        return mappedList;
    }

    static List<MgmtDistributionSet> toResponseFromDsList(final List<DistributionSet> sets) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return sets.stream().map(MgmtDistributionSetMapper::toResponse).collect(Collectors.toList());
    }
}
