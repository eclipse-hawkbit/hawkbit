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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition.Condition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction.ErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutSuccessAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutSuccessAction.SuccessAction;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.exception.ConstraintViolationException;
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
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(MgmtRolloutMapper::toResponseRollout).collect(Collectors.toList());
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
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).start(rollout.getId())).withRel("start"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).pause(rollout.getId())).withRel("pause"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).resume(rollout.getId())).withRel("resume"));
        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroups(rollout.getId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("groups"));
        return body;
    }

    static Rollout fromRequest(final EntityFactory entityFactory, final MgmtRolloutRestRequestBody restRequest,
            final DistributionSet distributionSet) {
        final Rollout rollout = entityFactory.generateRollout();
        rollout.setName(restRequest.getName());
        rollout.setDescription(restRequest.getDescription());
        rollout.setDistributionSet(distributionSet);
        rollout.setTargetFilterQuery(restRequest.getTargetFilterQuery());
        final ActionType convertActionType = MgmtRestModelMapper.convertActionType(restRequest.getType());
        if (convertActionType != null) {
            rollout.setActionType(convertActionType);
        }
        if (restRequest.getForcetime() != null) {
            rollout.setForcedTime(restRequest.getForcetime());

        }
        return rollout;
    }

    static RolloutGroup fromRequest(final EntityFactory entityFactory, final MgmtRolloutGroup restRequest) {
        final RolloutGroup group = entityFactory.generateRolloutGroup();
        group.setName(restRequest.getName());
        group.setDescription(restRequest.getDescription());

        if (restRequest.getTargetFilterQuery() != null) {
            group.setTargetFilterQuery(restRequest.getTargetFilterQuery());
        }

        final Float targetPercentage = restRequest.getTargetPercentage();
        if (targetPercentage == null) {
            group.setTargetPercentage(100);
        } else if (targetPercentage <= 0 || targetPercentage > 100) {
            throw new ConstraintViolationException("Target percentage out of Range >0 - 100.");
        } else {
            group.setTargetPercentage(restRequest.getTargetPercentage());
        }

        if (restRequest.getSuccessCondition() != null) {
            group.setSuccessCondition(mapFinishCondition(restRequest.getSuccessCondition().getCondition()));
            group.setSuccessConditionExp(restRequest.getSuccessCondition().getExpression());
        }
        if (restRequest.getSuccessAction() != null) {
            group.setSuccessAction(map(restRequest.getSuccessAction().getAction()));
            group.setSuccessActionExp(restRequest.getSuccessAction().getExpression());
        }
        if (restRequest.getErrorCondition() != null) {
            group.setErrorCondition(mapErrorCondition(restRequest.getErrorCondition().getCondition()));
            group.setErrorConditionExp(restRequest.getErrorCondition().getExpression());
        }
        if (restRequest.getErrorAction() != null) {
            group.setErrorAction(map(restRequest.getErrorAction().getAction()));
            group.setErrorActionExp(restRequest.getErrorAction().getExpression());
        }

        return group;
    }

    static List<MgmtRolloutGroupResponseBody> toResponseRolloutGroup(final List<RolloutGroup> rollouts) {
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(MgmtRolloutMapper::toResponseRolloutGroup).collect(Collectors.toList());
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
        body.setTargetPercentage(rolloutGroup.getTargetPercentage());
        body.setTargetFilterQuery(rolloutGroup.getTargetFilterQuery());
        body.setTotalTargets(rolloutGroup.getTotalTargets());

        body.setSuccessCondition(new MgmtRolloutCondition(map(rolloutGroup.getSuccessCondition()),
                rolloutGroup.getSuccessConditionExp()));
        body.setSuccessAction(
                new MgmtRolloutSuccessAction(map(rolloutGroup.getSuccessAction()), rolloutGroup.getSuccessActionExp()));

        body.setErrorCondition(
                new MgmtRolloutCondition(map(rolloutGroup.getErrorCondition()), rolloutGroup.getErrorConditionExp()));
        body.setErrorAction(
                new MgmtRolloutErrorAction(map(rolloutGroup.getErrorAction()), rolloutGroup.getErrorActionExp()));

        for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
            body.getTotalTargetsPerStatus().put(status.name().toLowerCase(),
                    rolloutGroup.getTotalTargetCountStatus().getTotalTargetCountByStatus(status));
        }

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

    static Condition map(final RolloutGroupErrorCondition rolloutCondition) {
        if (RolloutGroupErrorCondition.THRESHOLD.equals(rolloutCondition)) {
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

    static SuccessAction map(final RolloutGroupSuccessAction successAction) {
        if (RolloutGroupSuccessAction.NEXTGROUP.equals(successAction)) {
            return SuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Rollout group success action " + successAction + NOT_SUPPORTED);
    }

    static ErrorAction map(final RolloutGroupErrorAction errorAction) {
        if (RolloutGroupErrorAction.PAUSE.equals(errorAction)) {
            return ErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Rollout group error action " + errorAction + NOT_SUPPORTED);
    }

    private static String createIllegalArgumentLiteral(final Condition condition) {
        return "Condition " + condition + NOT_SUPPORTED;
    }

}
