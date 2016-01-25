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

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.rest.resource.helper.RestResourceConversionHelper;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutCondition.Condition;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutErrorAction.ErrorAction;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutResponseBody;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutRestRequestBody;
import org.eclipse.hawkbit.rest.resource.model.rollout.RolloutSuccessAction.SuccessAction;
import org.eclipse.hawkbit.rest.resource.model.rolloutgroup.RolloutGroupResponseBody;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 * @author Kai Zimmermann
 * @since 0.4.0
 *
 */
final class RolloutMapper {
    private RolloutMapper() {
        // Utility class
    }

    static List<RolloutResponseBody> toResponseRollout(final List<Rollout> rollouts) {
        final List<RolloutResponseBody> result = new ArrayList<>(rollouts.size());
        rollouts.forEach(r -> result.add(toResponseRollout(r)));
        return result;
    }

    static RolloutResponseBody toResponseRollout(final Rollout rollout) {
        final RolloutResponseBody body = new RolloutResponseBody();
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
        body.add(linkTo(methodOn(RolloutResource.class).getRollout(rollout.getId())).withRel("self"));
        body.add(linkTo(methodOn(RolloutResource.class).start(rollout.getId())).withRel("start"));
        body.add(linkTo(methodOn(RolloutResource.class).pause(rollout.getId())).withRel("pause"));
        body.add(linkTo(methodOn(RolloutResource.class).resume(rollout.getId())).withRel("pause"));
        body.add(linkTo(
                methodOn(RolloutResource.class).getRolloutGroups(rollout.getId(),
                        Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                        Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null)).withRel(
                "groups"));
        return body;
    }

    static Rollout fromRequest(final RolloutRestRequestBody restRequest, final DistributionSet distributionSet,
            final String filterQuery) {
        final Rollout rollout = new Rollout();
        rollout.setName(restRequest.getName());
        rollout.setDescription(restRequest.getDescription());
        rollout.setDistributionSet(distributionSet);
        rollout.setTargetFilterQuery(filterQuery);
        final ActionType convertActionType = RestResourceConversionHelper.convertActionType(restRequest.getType());
        if (convertActionType != null) {
            rollout.setActionType(convertActionType);
        }
        if (restRequest.getForcetime() != null) {
            rollout.setForcedTime(restRequest.getForcetime());

        }
        return rollout;
    }

    static List<RolloutGroupResponseBody> toResponseRolloutGroup(final List<RolloutGroup> rollouts) {
        final List<RolloutGroupResponseBody> result = new ArrayList<>(rollouts.size());
        rollouts.forEach(r -> result.add(toResponseRolloutGroup(r)));
        return result;
    }

    static RolloutGroupResponseBody toResponseRolloutGroup(final RolloutGroup rolloutGroup) {
        final RolloutGroupResponseBody body = new RolloutGroupResponseBody();
        body.setCreatedAt(rolloutGroup.getCreatedAt());
        body.setCreatedBy(rolloutGroup.getCreatedBy());
        body.setDescription(rolloutGroup.getDescription());
        body.setLastModifiedAt(rolloutGroup.getLastModifiedAt());
        body.setLastModifiedBy(rolloutGroup.getLastModifiedBy());
        body.setName(rolloutGroup.getName());
        body.setRolloutGroupId(rolloutGroup.getId());
        body.setStatus(rolloutGroup.getStatus().toString().toLowerCase());
        body.add(linkTo(
                methodOn(RolloutResource.class)
                        .getRolloutGroup(rolloutGroup.getRollout().getId(), rolloutGroup.getId())).withRel("self"));
        return body;
    }

    static RolloutGroupErrorCondition mapErrorCondition(final Condition condition) {
        switch (condition) {
        case THRESHOLD:
            return RolloutGroupErrorCondition.THRESHOLD;
        default:
            throw new IllegalArgumentException("Condition " + condition + " is not supported");
        }
    }

    static RolloutGroupSuccessCondition mapFinishCondition(final Condition condition) {
        switch (condition) {
        case THRESHOLD:
            return RolloutGroupSuccessCondition.THRESHOLD;
        default:
            throw new IllegalArgumentException("Condition " + condition + " is not supported");
        }
    }

    static Condition map(final RolloutGroupSuccessCondition rolloutCondition) {
        switch (rolloutCondition) {
        case THRESHOLD:
            return Condition.THRESHOLD;
        default:
            throw new IllegalArgumentException("Condition " + rolloutCondition + " is not supported");
        }
    }

    static RolloutGroupErrorAction map(final ErrorAction action) {
        switch (action) {
        case PAUSE:
            return RolloutGroupErrorAction.PAUSE;
        default:
            throw new IllegalArgumentException("Error Action " + action + " is not supported");
        }
    }

    static RolloutGroupSuccessAction map(final SuccessAction action) {
        switch (action) {
        case NEXTGROUP:
            return RolloutGroupSuccessAction.NEXTGROUP;
        default:
            throw new IllegalArgumentException("Success Action " + action + " is not supported");
        }
    }
}
