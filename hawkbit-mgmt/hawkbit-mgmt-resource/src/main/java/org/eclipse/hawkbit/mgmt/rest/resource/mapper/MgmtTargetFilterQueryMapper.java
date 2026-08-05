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

import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.UpdateCreate;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtTargetFilterQueryMapper {

    public static MgmtTargetFilterQuery toResponse(
            final TargetFilterQuery filter, final Optional<AutoAssignment> autoAssignment,
            final boolean confirmationFlowEnabled, final boolean isRepresentationFull) {
        final MgmtTargetFilterQuery targetRest = new MgmtTargetFilterQuery();
        targetRest.setId(filter.getId());
        targetRest.setName(filter.getName());
        targetRest.setQuery(filter.getQuery());

        targetRest.setCreatedBy(filter.getCreatedBy());
        targetRest.setLastModifiedBy(filter.getLastModifiedBy());

        targetRest.setCreatedAt(filter.getCreatedAt());
        targetRest.setLastModifiedAt(filter.getLastModifiedAt());

        final DistributionSet distributionSet = autoAssignment.map(AutoAssignment::getDistributionSet).orElse(null);
        if (distributionSet != null) {
            targetRest.setAutoAssignDistributionSet(distributionSet.getId());
            if (autoAssignment.isPresent()) {
                targetRest.setAutoAssignActionType(MgmtRestModelMapper.convertActionType(autoAssignment.get().getActionType()));
                autoAssignment.get().getWeight().ifPresent(targetRest::setAutoAssignWeight);
            }
            if (confirmationFlowEnabled) {
                targetRest.setConfirmationRequired(autoAssignment.get().isConfirmationRequired());
            }
        }

        targetRest.add(
                linkTo(methodOn(MgmtTargetFilterQueryRestApi.class).getFilter(filter.getId())).withSelfRel().expand());
        if (isRepresentationFull && distributionSet != null) {
            targetRest.add(
                    linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSets(
                            "name==" + distributionSet.getName() + ";version==" + distributionSet.getVersion(),
                            Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                            Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null
                    )).withRel("DS").expand());
        }

        return targetRest;
    }

    public static void addLinks(final MgmtTargetFilterQuery targetRest) {
        targetRest.add(linkTo(methodOn(MgmtTargetFilterQueryRestApi.class)
                .postAssignedDistributionSet(targetRest.getId(), null)).withRel("autoAssignDS").expand());
    }

    public static UpdateCreate fromRequest(final MgmtTargetFilterQueryRequestBody filterRest) {
        return UpdateCreate.builder().name(filterRest.getName()).query(filterRest.getQuery()).build();
    }
}