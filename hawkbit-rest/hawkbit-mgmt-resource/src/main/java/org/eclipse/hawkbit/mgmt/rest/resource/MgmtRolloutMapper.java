/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.rollout.AbstractMgmtRolloutConditionsEntity;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutCondition.Condition;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutErrorAction.ErrorAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutRestRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutSuccessAction;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutSuccessAction.SuccessAction;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtDynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroup;
import org.eclipse.hawkbit.mgmt.json.model.rolloutgroup.MgmtRolloutGroupResponseBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DynamicRolloutGroupTemplate;
import org.eclipse.hawkbit.repository.builder.RolloutCreate;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
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
        return toResponseRollout(rollouts, false);
    }

    static List<MgmtRolloutResponseBody> toResponseRollout(final List<Rollout> rollouts, final boolean withDetails) {
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(rollout -> toResponseRollout(rollout, withDetails)).collect(Collectors.toList());
    }

    static MgmtRolloutResponseBody toResponseRollout(final Rollout rollout, final boolean withDetails) {
        final MgmtRolloutResponseBody body = new MgmtRolloutResponseBody();
        body.setCreatedAt(rollout.getCreatedAt());
        body.setCreatedBy(rollout.getCreatedBy());
        body.setDescription(rollout.getDescription());
        body.setLastModifiedAt(rollout.getLastModifiedAt());
        body.setLastModifiedBy(rollout.getLastModifiedBy());
        body.setName(rollout.getName());
        body.setRolloutId(rollout.getId());
        body.setDynamic(rollout.isDynamic());
        body.setTargetFilterQuery(rollout.getTargetFilterQuery());
        body.setDistributionSetId(rollout.getDistributionSet().getId());
        body.setStatus(rollout.getStatus().toString().toLowerCase());
        body.setTotalTargets(rollout.getTotalTargets());
        body.setDeleted(rollout.isDeleted());
        body.setType(MgmtRestModelMapper.convertActionType(rollout.getActionType()));
        body.setForcetime(rollout.getForcedTime());
        rollout.getWeight().ifPresent(body::setWeight);

        if (withDetails) {
            for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
                body.addTotalTargetsPerStatus(status.name().toLowerCase(),
                        rollout.getTotalTargetCountStatus().getTotalTargetCountByStatus(status));
            }
            body.setTotalGroups(rollout.getRolloutGroupsCreated());
            body.setStartAt(rollout.getStartAt());

            body.setApproveDecidedBy(rollout.getApprovalDecidedBy());
            body.setApprovalRemark(rollout.getApprovalRemark());

            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).start(rollout.getId())).withRel("start").expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).pause(rollout.getId())).withRel("pause").expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).resume(rollout.getId())).withRel("resume").expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).triggerNextGroup(rollout.getId()))
                    .withRel("triggerNextGroup").expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).approve(rollout.getId(), null)).withRel("approve")
                    .expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).deny(rollout.getId(), null)).withRel("deny").expand());
            body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroups(rollout.getId(),
                    MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                    MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null, null)).withRel("groups")
                            .expand());

            final DistributionSet distributionSet = rollout.getDistributionSet();
            body.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(distributionSet.getId()))
                    .withRel("distributionset").withName(distributionSet.getName() + ":" + distributionSet.getVersion())
                    .expand());
        }

        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRollout(rollout.getId())).withSelfRel().expand());
        return body;
    }

    static RolloutCreate fromRequest(final EntityFactory entityFactory, final MgmtRolloutRestRequestBodyPost restRequest,
            final DistributionSet distributionSet) {
        return entityFactory.rollout().create()
                .name(restRequest.getName())
                .description(restRequest.getDescription())
                .distributionSetId(distributionSet)
                .targetFilterQuery(restRequest.getTargetFilterQuery())
                .actionType(MgmtRestModelMapper.convertActionType(restRequest.getType()))
                .forcedTime(restRequest.getForcetime()).startAt(restRequest.getStartAt())
                .weight(restRequest.getWeight())
                .dynamic(restRequest.isDynamic());
    }

    static RolloutUpdate fromRequest(final EntityFactory entityFactory, final MgmtRolloutRestRequestBodyPut restRequest, final long rolloutId) {
        return entityFactory.rollout().update(rolloutId)
                .name(restRequest.getName())
                .description(restRequest.getDescription());
    }

    static RolloutCreate fromRetriedRollout(final EntityFactory entityFactory, final Rollout rollout) {
        return entityFactory.rollout().create()
                .name(rollout.getName().concat("_retry"))
                .description(rollout.getDescription())
                .distributionSetId(rollout.getDistributionSet())
                .targetFilterQuery("failedrollout==".concat(String.valueOf(rollout.getId())))
                .actionType(rollout.getActionType())
                .forcedTime(rollout.getForcedTime())
                .startAt(rollout.getStartAt())
                .weight(null);
    }

    static RolloutGroupCreate fromRequest(final EntityFactory entityFactory, final MgmtRolloutGroup restRequest) {
        return entityFactory.rolloutGroup().create().name(restRequest.getName())
                .description(restRequest.getDescription()).targetFilterQuery(restRequest.getTargetFilterQuery())
                .targetPercentage(restRequest.getTargetPercentage()).conditions(fromRequest(restRequest, false));
    }

    static DynamicRolloutGroupTemplate fromRequest(final MgmtDynamicRolloutGroupTemplate restRequest) {
        if (restRequest == null) {
            return null;
        }
        return DynamicRolloutGroupTemplate.builder()
                .nameSuffix(Optional.ofNullable(restRequest.getNameSuffix()).orElse(""))
                .targetCount(restRequest.getTargetCount())
                .build();
    }

    static RolloutGroupConditions fromRequest(final AbstractMgmtRolloutConditionsEntity restRequest,
            final boolean withDefaults) {
        final RolloutGroupConditionBuilder conditions = new RolloutGroupConditionBuilder();

        if (withDefaults) {
            conditions.withDefaults();
        }

        if (restRequest.getSuccessCondition() != null) {
            conditions.successCondition(mapFinishCondition(restRequest.getSuccessCondition().getCondition()),
                    restRequest.getSuccessCondition().getExpression());
        }
        if (restRequest.getSuccessAction() != null) {
            conditions.successAction(map(restRequest.getSuccessAction().getAction()),
                    restRequest.getSuccessAction().getExpression());
        }

        if (restRequest.getErrorCondition() != null) {
            conditions.errorCondition(mapErrorCondition(restRequest.getErrorCondition().getCondition()),
                    restRequest.getErrorCondition().getExpression());
        }
        if (restRequest.getErrorAction() != null) {
            conditions.errorAction(map(restRequest.getErrorAction().getAction()),
                    restRequest.getErrorAction().getExpression());
        }

        return conditions.build();
    }

    static List<MgmtRolloutGroupResponseBody> toResponseRolloutGroup(final List<RolloutGroup> rollouts,
            final boolean confirmationFlowEnabled, final boolean withDetails) {
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(group -> toResponseRolloutGroup(group, withDetails, confirmationFlowEnabled))
                .collect(Collectors.toList());
    }

    static MgmtRolloutGroupResponseBody toResponseRolloutGroup(final RolloutGroup rolloutGroup,
            final boolean withDetailedStatus, final boolean confirmationFlowEnabled) {
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

        if (confirmationFlowEnabled) {
            body.setConfirmationRequired(rolloutGroup.isConfirmationRequired());
        }

        body.setDynamic(rolloutGroup.isDynamic());

        body.setSuccessCondition(new MgmtRolloutCondition(map(rolloutGroup.getSuccessCondition()),
                rolloutGroup.getSuccessConditionExp()));
        body.setSuccessAction(
                new MgmtRolloutSuccessAction(map(rolloutGroup.getSuccessAction()), rolloutGroup.getSuccessActionExp()));

        body.setErrorCondition(
                new MgmtRolloutCondition(map(rolloutGroup.getErrorCondition()), rolloutGroup.getErrorConditionExp()));
        body.setErrorAction(
                new MgmtRolloutErrorAction(map(rolloutGroup.getErrorAction()), rolloutGroup.getErrorActionExp()));

        if (withDetailedStatus) {
            for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
                body.addTotalTargetsPerStatus(status.name().toLowerCase(),
                        rolloutGroup.getTotalTargetCountStatus().getTotalTargetCountByStatus(status));
            }
        }

        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroup(rolloutGroup.getRollout().getId(),
                rolloutGroup.getId())).withSelfRel());
        return body;
    }

    private static RolloutGroupErrorCondition mapErrorCondition(final Condition condition) {
        if (Condition.THRESHOLD == condition) {
            return RolloutGroupErrorCondition.THRESHOLD;
        }
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    private static RolloutGroupSuccessCondition mapFinishCondition(final Condition condition) {
        if (Condition.THRESHOLD == condition) {
            return RolloutGroupSuccessCondition.THRESHOLD;
        }
        throw new IllegalArgumentException(createIllegalArgumentLiteral(condition));
    }

    private static Condition map(final RolloutGroupSuccessCondition rolloutCondition) {
        if (RolloutGroupSuccessCondition.THRESHOLD == rolloutCondition) {
            return Condition.THRESHOLD;
        }
        throw new IllegalArgumentException("Rollout group condition " + rolloutCondition + NOT_SUPPORTED);
    }

    private static Condition map(final RolloutGroupErrorCondition rolloutCondition) {
        if (RolloutGroupErrorCondition.THRESHOLD == rolloutCondition) {
            return Condition.THRESHOLD;
        }
        throw new IllegalArgumentException("Rollout group condition " + rolloutCondition + NOT_SUPPORTED);
    }

    private static RolloutGroupErrorAction map(final ErrorAction action) {
        if (ErrorAction.PAUSE == action) {
            return RolloutGroupErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Error Action " + action + NOT_SUPPORTED);
    }

    private static RolloutGroupSuccessAction map(final SuccessAction action) {
        if (SuccessAction.NEXTGROUP == action) {
            return RolloutGroupSuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Success Action " + action + NOT_SUPPORTED);
    }

    private static SuccessAction map(final RolloutGroupSuccessAction successAction) {
        if (RolloutGroupSuccessAction.NEXTGROUP == successAction) {
            return SuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Rollout group success action " + successAction + NOT_SUPPORTED);
    }

    private static ErrorAction map(final RolloutGroupErrorAction errorAction) {
        if (RolloutGroupErrorAction.PAUSE == errorAction) {
            return ErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Rollout group error action " + errorAction + NOT_SUPPORTED);
    }

    private static String createIllegalArgumentLiteral(final Condition condition) {
        return "Condition " + condition + NOT_SUPPORTED;
    }
}
