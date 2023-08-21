/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtDistributionSetAutoAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.hateoas.Link;
import org.springframework.util.CollectionUtils;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtTargetFilterQueryMapper {

    private MgmtTargetFilterQueryMapper() {
        // Utility class
    }

    static List<MgmtTargetFilterQuery> toResponse(final List<TargetFilterQuery> filters,
            final boolean confirmationFlowEnabled, final boolean isRepresentationFull) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }
        return filters.stream().map(filter -> toResponse(filter, confirmationFlowEnabled, isRepresentationFull)).collect(Collectors.toList());
    }

    static MgmtTargetFilterQuery toResponse(final TargetFilterQuery filter, final boolean confirmationFlowEnabled,
        final boolean isReprentationFull) {
        final MgmtTargetFilterQuery targetRest = new MgmtTargetFilterQuery();
        targetRest.setFilterId(filter.getId());
        targetRest.setName(filter.getName());
        targetRest.setQuery(filter.getQuery());

        targetRest.setCreatedBy(filter.getCreatedBy());
        targetRest.setLastModifiedBy(filter.getLastModifiedBy());

        targetRest.setCreatedAt(filter.getCreatedAt());
        targetRest.setLastModifiedAt(filter.getLastModifiedAt());

        final DistributionSet distributionSet = filter.getAutoAssignDistributionSet();
        if (distributionSet != null) {
            targetRest.setAutoAssignDistributionSet(distributionSet.getId());
            targetRest.setAutoAssignActionType(MgmtRestModelMapper.convertActionType(filter.getAutoAssignActionType()));
            filter.getAutoAssignWeight().ifPresent(targetRest::setAutoAssignWeight);
            if (confirmationFlowEnabled) {
                targetRest.setConfirmationRequired(filter.isConfirmationRequired());
            }
        }

        targetRest.add(
                linkTo(methodOn(MgmtTargetFilterQueryRestApi.class).getFilter(filter.getId())).withSelfRel().expand());
        if (isReprentationFull && distributionSet != null) {
            targetRest.add(
                linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSets(Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                    Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null,
                    "name==" + distributionSet.getName() + ";version==" + distributionSet.getVersion())).withRel("DS").expand());
        }

        return targetRest;
    }

    static void addLinks(final MgmtTargetFilterQuery targetRest) {
        targetRest.add(linkTo(methodOn(MgmtTargetFilterQueryRestApi.class)
                .postAssignedDistributionSet(targetRest.getFilterId(), null)).withRel("autoAssignDS").expand());
    }

    static TargetFilterQueryCreate fromRequest(final EntityFactory entityFactory,
            final MgmtTargetFilterQueryRequestBody filterRest) {

        return entityFactory.targetFilterQuery().create().name(filterRest.getName()).query(filterRest.getQuery());
    }

    static AutoAssignDistributionSetUpdate fromRequest(final EntityFactory entityFactory, final long filterId,
            final MgmtDistributionSetAutoAssignment assignRest) {
        final ActionType type = MgmtRestModelMapper.convertActionType(assignRest.getType());

        return entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(assignRest.getId()).actionType(type)
                .weight(assignRest.getWeight());
    }

}
