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
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtDistributionSetMapper {
    private MgmtDistributionSetMapper() {
        // Utility class
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost}s to {@link DistributionSet}s.
     *
     * @param sets
     *            to convert
     * @return converted list of {@link DistributionSet}s
     */
    static List<DistributionSetCreate> dsFromRequest(final Collection<MgmtDistributionSetRequestBodyPost> sets,
            final EntityFactory entityFactory) {

        return sets.stream().map(dsRest -> fromRequest(dsRest, entityFactory)).collect(Collectors.toList());
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost} to {@link DistributionSet}.
     *
     * @param dsRest
     *            to convert
     * @return converted {@link DistributionSet}
     */
    private static DistributionSetCreate fromRequest(final MgmtDistributionSetRequestBodyPost dsRest,
            final EntityFactory entityFactory) {

        final List<Long> modules = new ArrayList<>();

        if (dsRest.getOs() != null) {
            modules.add(dsRest.getOs().getId());
        }

        if (dsRest.getApplication() != null) {
            modules.add(dsRest.getApplication().getId());
        }

        if (dsRest.getRuntime() != null) {
            modules.add(dsRest.getRuntime().getId());
        }

        if (dsRest.getModules() != null) {
            dsRest.getModules().forEach(module -> modules.add(module.getId()));
        }

        return entityFactory.distributionSet().create().name(dsRest.getName()).version(dsRest.getVersion())
                .description(dsRest.getDescription()).type(dsRest.getType()).modules(modules)
                .requiredMigrationStep(dsRest.isRequiredMigrationStep());
    }

    static List<MetaData> fromRequestDsMetadata(final List<MgmtMetadata> metadata, final EntityFactory entityFactory) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream()
                .map(metadataRest -> entityFactory.generateMetadata(metadataRest.getKey(), metadataRest.getValue()))
                .collect(Collectors.toList());
    }

    static MgmtDistributionSet toResponse(final DistributionSet distributionSet) {
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
                .withSelfRel());

        return response;
    }

    static void addLinks(final DistributionSet distributionSet, final MgmtDistributionSet response) {
        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getAssignedSoftwareModules(response.getDsId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null))
                        .withRel(MgmtRestConstants.DISTRIBUTIONSET_V1_MODULE));

        response.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(distributionSet.getType().getId())).withRel("type"));

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(response.getDsId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("metadata"));
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

        return new ResponseList<>(
                sets.stream().map(MgmtDistributionSetMapper::toResponse).collect(Collectors.toList()));
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
