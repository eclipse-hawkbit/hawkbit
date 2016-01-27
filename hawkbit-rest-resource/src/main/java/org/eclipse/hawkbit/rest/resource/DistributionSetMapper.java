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

import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.resource.model.MetadataRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRequestBodyPost;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.DistributionSetsRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.TargetAssignmentResponseBody;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 *
 *
 */
public final class DistributionSetMapper {
    private DistributionSetMapper() {
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
     * {@link DistributionSetRequestBodyPost}s to {@link DistributionSet}s.
     *
     * @param sets
     *            to convert
     * @param softwareManagement
     *            to use for conversion
     * @return converted list of {@link DistributionSet}s
     */
    static List<DistributionSet> dsFromRequest(final Iterable<DistributionSetRequestBodyPost> sets,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement) {

        final List<DistributionSet> mappedList = new ArrayList<>();
        for (final DistributionSetRequestBodyPost dsRest : sets) {
            mappedList.add(fromRequest(dsRest, softwareManagement, distributionSetManagement));
        }
        return mappedList;

    }

    /**
     * {@link DistributionSetRequestBodyPost} to {@link DistributionSet}.
     *
     * @param dsRest
     *            to convert
     * @param softwareManagement
     *            to use for conversion
     * @return converted {@link DistributionSet}
     */
    static DistributionSet fromRequest(final DistributionSetRequestBodyPost dsRest,
            final SoftwareManagement softwareManagement, final DistributionSetManagement distributionSetManagement) {

        final DistributionSet result = new DistributionSet();
        result.setDescription(dsRest.getDescription());
        result.setName(dsRest.getName());
        result.setType(findDistributionSetTypeWithExceptionIfNotFound(dsRest.getType(), distributionSetManagement));

        result.setRequiredMigrationStep(dsRest.isRequiredMigrationStep());
        result.setVersion(dsRest.getVersion());

        if (dsRest.getOs() != null) {
            result.addModule(findSoftwareModuleWithExceptionIfNotFound(dsRest.getOs().getId(), softwareManagement));
        }

        if (dsRest.getApplication() != null) {
            result.addModule(
                    findSoftwareModuleWithExceptionIfNotFound(dsRest.getApplication().getId(), softwareManagement));
        }

        if (dsRest.getRuntime() != null) {
            result.addModule(
                    findSoftwareModuleWithExceptionIfNotFound(dsRest.getRuntime().getId(), softwareManagement));
        }

        if (dsRest.getModules() != null) {
            dsRest.getModules().forEach(module -> result
                    .addModule(findSoftwareModuleWithExceptionIfNotFound(module.getId(), softwareManagement)));
        }

        return result;
    }

    /**
     * From {@link MetadataRest} to {@link DistributionSetMetadata}.
     *
     * @param ds
     * @param metadata
     * @return
     */
    static List<DistributionSetMetadata> fromRequestDsMetadata(final DistributionSet ds,
            final List<MetadataRest> metadata) {
        final List<DistributionSetMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final MetadataRest metadataRest : metadata) {
            if (metadataRest.getKey() == null) {
                throw new IllegalArgumentException("the key of the metadata must be present");
            }
            mappedList.add(new DistributionSetMetadata(metadataRest.getKey(), ds, metadataRest.getValue()));
        }
        return mappedList;
    }

    /**
     * Create a response for distribution set.
     * 
     * @param distributionSet
     *            the ds set
     * @return the response
     */
    public static DistributionSetRest toResponse(final DistributionSet distributionSet) {
        if (distributionSet == null) {
            return null;
        }
        final DistributionSetRest response = new DistributionSetRest();
        RestModelMapper.mapNamedToNamed(response, distributionSet);

        response.setDsId(distributionSet.getId());
        response.setVersion(distributionSet.getVersion());
        response.setComplete(distributionSet.isComplete());
        response.setType(distributionSet.getType().getKey());

        distributionSet.getModules()
                .forEach(module -> response.getModules().add(SoftwareModuleMapper.toResponse(module)));

        response.setRequiredMigrationStep(distributionSet.isRequiredMigrationStep());

        response.add(
                linkTo(methodOn(DistributionSetResource.class).getDistributionSet(response.getDsId())).withRel("self"));

        response.add(linkTo(
                methodOn(DistributionSetTypeResource.class).getDistributionSetType(distributionSet.getType().getId()))
                        .withRel("type"));

        response.add(linkTo(methodOn(DistributionSetResource.class).getMetadata(response.getDsId(),
                Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));

        return response;
    }

    static TargetAssignmentResponseBody toResponse(final DistributionSetAssignmentResult dsAssignmentResult) {
        final TargetAssignmentResponseBody result = new TargetAssignmentResponseBody();
        result.setAssigned(dsAssignmentResult.getAssigned());
        result.setAlreadyAssigned(dsAssignmentResult.getAlreadyAssigned());
        result.setTotal(dsAssignmentResult.getTotal());
        return result;
    }

    static DistributionSetsRest toResponseDistributionSets(final Iterable<DistributionSet> sets) {
        final DistributionSetsRest response = new DistributionSetsRest();
        if (sets != null) {

            for (final DistributionSet set : sets) {
                response.add(toResponse(set));
            }
        }
        return response;
    }

    static MetadataRest toResponseDsMetadata(final DistributionSetMetadata metadata) {
        final MetadataRest metadataRest = new MetadataRest();
        metadataRest.setKey(metadata.getId().getKey());
        metadataRest.setValue(metadata.getValue());
        return metadataRest;
    }

    static List<MetadataRest> toResponseDsMetadata(final List<DistributionSetMetadata> metadata) {

        final List<MetadataRest> mappedList = new ArrayList<>(metadata.size());
        for (final DistributionSetMetadata distributionSetMetadata : metadata) {
            mappedList.add(toResponseDsMetadata(distributionSetMetadata));
        }
        return mappedList;
    }

    static List<DistributionSetRest> toResponseFromDsList(final List<DistributionSet> sets) {
        final List<DistributionSetRest> mappedList = new ArrayList<>();
        if (sets != null) {
            for (final DistributionSet set : sets) {
                final DistributionSetRest response = toResponse(set);

                mappedList.add(response);
            }
        }
        return mappedList;
    }
}
