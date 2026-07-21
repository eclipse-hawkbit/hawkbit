/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.eclipse.hawkbit.repository.model.Action.ActionType.FORCED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.PAUSED;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.RUNNING;
import static org.eclipse.hawkbit.repository.model.TargetFilterQuery.AutoAssignStatus.WAITING_FOR_APPROVAL;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtAutoAssignmentRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MgmtAutoAssignmentMapper {

    public static List<MgmtAutoAssignmentResponseBody> toResponseAutoAssignment(final List<TargetFilterQuery> targetFilterQueries) {
        if (targetFilterQueries == null) {
            return Collections.emptyList();
        }

        return targetFilterQueries.stream().map(MgmtAutoAssignmentMapper::toResponseAutoAssignment).toList();
    }

    public static MgmtAutoAssignmentResponseBody toResponseAutoAssignment(final TargetFilterQuery targetFilterQuery) {
        if (targetFilterQuery.getAutoAssignDistributionSet() == null) {
            throw new EntityNotFoundException("Distribution Set inside Target Filter Query", targetFilterQuery.getId());
        }
        final MgmtAutoAssignmentResponseBody body = new MgmtAutoAssignmentResponseBody();
        body.setId(targetFilterQuery.getId());
        body.setDistributionSetId(targetFilterQuery.getAutoAssignDistributionSet().getId());
        body.setTargetFilterQuery(targetFilterQuery.getQuery());
        body.setStatus(targetFilterQuery.getAutoAssignStatus().toString().toLowerCase());
        body.setStartAt(targetFilterQuery.getStartAt());
        body.setActionType(MgmtRestModelMapper.convertActionType(targetFilterQuery.getAutoAssignActionType()));
        targetFilterQuery.getAutoAssignWeight().ifPresent(body::setWeight);
        body.setConfirmationRequired(targetFilterQuery.isConfirmationRequired());
        body.setApprovalDecidedBy(targetFilterQuery.getApprovalDecidedBy());
        body.setApprovalRemark(targetFilterQuery.getApprovalRemark());

        body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).getAutoAssignment(body.getId())).withSelfRel().expand());
        if (targetFilterQuery.getAutoAssignStatus() == WAITING_FOR_APPROVAL) {
            body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).approve(body.getId(), null)).withRel("approve").expand());
            body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).deny(body.getId(), null)).withRel("deny").expand());
        }
        else if (targetFilterQuery.getAutoAssignStatus() == READY) {
            body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).start(body.getId())).withRel("start").expand());
        }
        else if (targetFilterQuery.getAutoAssignStatus() == RUNNING) {
            body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).pause(body.getId())).withRel("pause").expand());
        }
        else if (targetFilterQuery.getAutoAssignStatus() == PAUSED) {
            body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).resume(body.getId())).withRel("resume").expand());
        }

        final DistributionSet distributionSet = targetFilterQuery.getAutoAssignDistributionSet();
        body.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(distributionSet.getId()))
                .withRel("distributionset").withName(distributionSet.getName() + ":" + distributionSet.getVersion()).expand());

        return body;
    }

    public static AutoAssignDistributionSetUpdate fromRequest(final MgmtAutoAssignmentRestRequestBodyPost restRequest,
            final Long targetFilterQueryId) {
        return new AutoAssignDistributionSetUpdate(targetFilterQueryId)
                .ds(restRequest.getDistributionSetId())
                .startAt(restRequest.getStartAt())
                .actionType(Optional.ofNullable(MgmtRestModelMapper.convertActionType(restRequest.getActionType())).orElse(
                        FORCED))
                .confirmationRequired(Optional.ofNullable(restRequest.getConfirmationRequired()).orElse(TenantConfigHelper
                        .isUserConfirmationFlowEnabled()))
                .weight(restRequest.getWeight());
    }
}
