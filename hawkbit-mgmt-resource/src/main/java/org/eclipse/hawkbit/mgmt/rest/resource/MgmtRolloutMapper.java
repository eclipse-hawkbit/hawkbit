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
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition.Condition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction.ErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutSuccessAction.SuccessAction;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 *
 */
final class MgmtRolloutMapper {

    private static final String NOT_SUPPORTED = " is not supported";

    private MgmtRolloutMapper() {
        // Utility class
    }

    static List<MgmtRolloutResponseBody> toResponseRollout(final List<Rollout> rollouts) {
        final List<MgmtRolloutResponseBody> result = new ArrayList<>(rollouts.size());
        rollouts.forEach(r -> result.add(toResponseRollout(r)));
        return result;
    }

    static MgmtRolloutResponseBody toResponseRollout(final Rollout rollout) {
        final MgmtRolloutResponseBody body = new MgmtRolloutResponseBody();
        body.setCreatedAt(rollout.getCreatedAt());
        body.setCreatedBy(rollout.getCreatedBy());
        body.setDescription(rollout.getDescription());
        body.setLastModifiedAt(rollout.getLastModifiedAt());
        body.setLastModifiedBy(rollout.getLastModifiedBy());
        body.setName(rollout.getName());
        body.setRolloutId(rollout.getId());
        body.setTargetFilterQuery(rollout.getTargetFilterQuery());
        body.setDistributionSetId(rollout.getDistributionSet().getId());
        body.setStatus(rollout.getStatus().toString().toLowerCase());
        body.setTotalTargets(rollout.getTotalTargets());

        for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
            body.getTotalTargetsPerStatus().put(status.name().toLowerCase(),
                    rollout.getTotalTargetCountStatus().getTotalTargetCountByStatus(status));
        }

        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRollout(rollout.getId())).withRel("self"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).start(rollout.getId(), false)).withRel("start"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).start(rollout.getId(), true)).withRel("startAsync"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).pause(rollout.getId())).withRel("pause"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).resume(rollout.getId())).withRel("resume"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroups(rollout.getId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("groups"));
        return body;
    }

    static Rollout fromRequest(final MgmtRolloutRestRequestBody restRequest, final DistributionSet distributionSet,
            final String filterQuery) {
        final Rollout rollout = new Rollout();
        rollout.setName(restRequest.getName());
        rollout.setDescription(restRequest.getDescription());
        rollout.setDistributionSet(distributionSet);
        rollout.setTargetFilterQuery(filterQuery);
        final ActionType convertActionType = MgmtRestModelMapper.convertActionType(restRequest.getType());
        if (convertActionType != null) {
            rollout.setActionType(convertActionType);
        }
        if (restRequest.getForcetime() != null) {
            rollout.setForcedTime(restRequest.getForcetime());

        }
        return rollout;
    }

    static List<MgmtRolloutGroupResponseBody> toResponseRolloutGroup(final List<RolloutGroup> rollouts) {
        final List<MgmtRolloutGroupResponseBody> result = new ArrayList<>(rollouts.size());
        rollouts.forEach(r -> result.add(toResponseRolloutGroup(r)));
        return result;
    }

    static MgmtRolloutGroupResponseBody toResponseRolloutGroup(final RolloutGroup rolloutGroup) {
        final MgmtRolloutGroupResponseBody body = new MgmtRolloutGroupResponseBody();
        body.setCreatedAt(rolloutGroup.getCreatedAt());
        body.setCreatedBy(rolloutGroup.getCreatedBy());
        body.setDescription(rolloutGroup.getDescription());
        body.setLastModifiedAt(rolloutGroup.getLastModifiedAt());
        body.setLastModifiedBy(rolloutGroup.getLastModifiedBy());
        body.setName(rolloutGroup.getName());
        body.setRolloutGroupId(rolloutGroup.getId());
        body.setStatus(rolloutGroup.getStatus().toString().toLowerCase());
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroup(rolloutGroup.getRollout().getId(),
                rolloutGroup.getId())).withRel("self"));
        return body;
    }

    static RolloutGroupErrorCondition mapErrorCondition(final Condition condition) {
        if (Condition.THRESHOLD.equals(condition)) {
            return RolloutGroupErrorCondition.THRESHOLD;
        }
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    static RolloutGroupSuccessCondition mapFinishCondition(final Condition condition) {
        if (Condition.THRESHOLD.equals(condition)) {
            return RolloutGroupSuccessCondition.THRESHOLD;
        }
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    static Condition map(final RolloutGroupSuccessCondition rolloutCondition) {
        if (RolloutGroupSuccessCondition.THRESHOLD.equals(rolloutCondition)) {
            return Condition.THRESHOLD;
        }
        throw new IllegalArgumentException("Rollout group condition " + rolloutCondition + NOT_SUPPORTED);
    }

    static RolloutGroupErrorAction map(final ErrorAction action) {
        if (ErrorAction.PAUSE.equals(action)) {
            return RolloutGroupErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Error Action " + action + NOT_SUPPORTED);
    }

    static RolloutGroupSuccessAction map(final SuccessAction action) {
        if (SuccessAction.NEXTGROUP.equals(action)) {
            return RolloutGroupSuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Success Action " + action + NOT_SUPPORTED);
    }

    private static String createIllegalArgumentLiteral(final Condition condition) {
        return "Condition " + condition + NOT_SUPPORTED;
    }

}
