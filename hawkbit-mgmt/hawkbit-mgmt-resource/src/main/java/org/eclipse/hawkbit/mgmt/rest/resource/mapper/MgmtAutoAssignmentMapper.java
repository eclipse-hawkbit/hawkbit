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
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.autoassignment.MgmtAutoAssignmentRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtAutoAssignmentRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Create;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement.Update;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * A mapper which maps repository model to RESTful model representation and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MgmtAutoAssignmentMapper {

    public static List<MgmtAutoAssignmentResponseBody> toResponseAutoAssignment(final List<AutoAssignment> autoAssignments) {
        if (autoAssignments == null) {
            return Collections.emptyList();
        }

        return autoAssignments.stream().map(MgmtAutoAssignmentMapper::toResponseAutoAssignment).toList();
    }

    public static MgmtAutoAssignmentResponseBody toResponseAutoAssignment(final AutoAssignment autoAssignment) {

        final MgmtAutoAssignmentResponseBody body = new MgmtAutoAssignmentResponseBody();
        MgmtRestModelMapper.mapNamedToNamed(body, autoAssignment);
        body.setId(autoAssignment.getId());
        body.setDistributionSetId(autoAssignment.getDistributionSet().getId());
        body.setTargetFilterQuery(autoAssignment.getTargetFilterQuery());
        body.setStatus(autoAssignment.getStatus().toString().toLowerCase());
        body.setStartAt(autoAssignment.getStartAt());
        body.setActionType(MgmtRestModelMapper.convertActionType(autoAssignment.getActionType()));
        autoAssignment.getWeight().ifPresent(body::setWeight);
        body.setConfirmationRequired(autoAssignment.isConfirmationRequired());
        body.setApprovalDecidedBy(autoAssignment.getApprovalDecidedBy());
        body.setApprovalRemark(autoAssignment.getApprovalRemark());

        body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).getAutoAssignment(body.getId())).withSelfRel().expand());
        switch (autoAssignment.getStatus()) {
            case WAITING_FOR_APPROVAL -> {
                body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).approve(body.getId(), null)).withRel("approve").expand());
                body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).deny(body.getId(), null)).withRel("deny").expand());
            }
            case READY -> body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).start(body.getId())).withRel("start").expand());
            case RUNNING -> body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).pause(body.getId())).withRel("pause").expand());
            case PAUSED -> body.add(linkTo(methodOn(MgmtAutoAssignmentRestApi.class).resume(body.getId())).withRel("resume").expand());
            case APPROVAL_DENIED -> { /* terminal state, no action links */ }
        }

        final DistributionSet distributionSet = autoAssignment.getDistributionSet();
        body.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(distributionSet.getId()))
                .withRel("distributionset").withName(distributionSet.getName() + ":" + distributionSet.getVersion()).expand());

        return body;
    }

    public static Create fromRequest(final MgmtAutoAssignmentRestRequestBodyPost restRequest,
            final DistributionSet distributionSet) {
        return Create.builder()
                .distributionSet(distributionSet)
                .targetFilterQuery(restRequest.getTargetFilterQuery())
                .name(restRequest.getName())
                .description(restRequest.getDescription())
                .startAt(restRequest.getStartAt())
                .actionType(Optional.ofNullable(MgmtRestModelMapper.convertActionType(restRequest.getActionType())).orElse(
                        FORCED))
                .confirmationRequired(Optional.ofNullable(restRequest.getConfirmationRequired()).orElse(TenantConfigHelper
                        .isUserConfirmationFlowEnabled()))
                .weight(restRequest.getWeight()).build();
    }

    public static Update fromRequest(final MgmtAutoAssignmentRestRequestBodyPut restRequest, final long id) {
        return Update.builder()
                .name(restRequest.getName())
                .description(restRequest.getDescription()).id(id).build();
    }
}
