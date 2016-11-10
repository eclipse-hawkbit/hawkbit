/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.exception.RolloutVerificationException;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;

import java.util.List;
import java.util.stream.Collectors;

final class RolloutHelper {
    private RolloutHelper() {
    }

    static void verifyRolloutGroupConditions(final RolloutGroupConditions conditions) {
        if (conditions.getSuccessCondition() == null) {
            throw new RolloutVerificationException("Rollout group is missing success condition");
        }
        if (conditions.getSuccessAction() == null) {
            throw new RolloutVerificationException("Rollout group is missing success action");
        }
    }

    static RolloutGroup verifyRolloutGroupHasConditions(final RolloutGroup group) {
        if (group.getSuccessCondition() == null) {
            throw new RolloutVerificationException("Rollout group is missing success condition");
        }
        if (group.getSuccessAction() == null) {
            throw new RolloutVerificationException("Rollout group is missing success action");
        }
        return group;
    }

    static RolloutGroup prepareRolloutGroupWithDefaultConditions(final RolloutGroup group,
                                                                 final RolloutGroupConditions conditions) {
        if (group.getSuccessCondition() == null) {
            group.setSuccessCondition(conditions.getSuccessCondition());
        }
        if (group.getSuccessConditionExp() == null) {
            group.setSuccessConditionExp(conditions.getSuccessConditionExp());
        }
        if (group.getSuccessAction() == null) {
            group.setSuccessAction(conditions.getSuccessAction());
        }
        if (group.getSuccessActionExp() == null) {
            group.setSuccessActionExp(conditions.getSuccessActionExp());
        }

        if (group.getErrorCondition() == null) {
            group.setErrorCondition(conditions.getErrorCondition());
        }
        if (group.getErrorConditionExp() == null) {
            group.setErrorConditionExp(conditions.getErrorConditionExp());
        }
        if (group.getErrorAction() == null) {
            group.setErrorAction(conditions.getErrorAction());
        }
        if (group.getErrorActionExp() == null) {
            group.setErrorActionExp(conditions.getErrorActionExp());
        }
        return group;
    }

    static void verifyRolloutGroupParameter(final int amountGroup) {
        if (amountGroup <= 0) {
            throw new RolloutVerificationException("the amountGroup must be greater than zero");
        } else if (amountGroup > 500) {
            throw new RolloutVerificationException("the amountGroup must not be greater than 500");
        }
    }

    static void verifyRolloutGroupTargetPercentage(final float percentage) {
        if (percentage <= 0) {
            throw new RolloutVerificationException("the percentage must be greater than zero");
        } else if (percentage > 100) {
            throw new RolloutVerificationException("the percentage must not be greater than 100");
        }
    }

    static String getTargetFilterQuery(final Rollout rollout) {
        if (rollout.getCreatedAt() != null) {
            return rollout.getTargetFilterQuery() + ";createdat=le=" + rollout.getCreatedAt().toString();
        }
        return rollout.getTargetFilterQuery();
    }

    static void verifyRolloutInStatus(final Rollout rollout, final Rollout.RolloutStatus status) {
        if (!rollout.getStatus().equals(status)) {
            throw new RolloutIllegalStateException("Rollout is not in status " + status.toString());
        }
    }

    static List<RolloutGroup> getGroupsByStatusIncludingGroup(final Rollout rollout,
            final RolloutGroup.RolloutGroupStatus status, final RolloutGroup group) {
        return rollout.getRolloutGroups().stream().filter(g -> g.getStatus().equals(status) || g.equals(group))
                .collect(Collectors.toList());
    }

    static List<RolloutGroup> getOrderedGroups(final Rollout rollout) {
        return rollout.getRolloutGroups().stream().sorted((o1, o2) -> {
            if(o1.getId()<o2.getId()) {
                return -1;
            }
            if(o1.getId()>o2.getId()) {
                return 1;
            }
            return 0;
        }).collect(Collectors.toList());
    }

    /**
     * Creates an RSQL expression that matches all targets in the provided groups.
     * Links all target filter queries with OR.
     *
     * @param groups the rollout groups
     * @return RSQL string without base filter of the Rollout. Can be an empty string.
     */
    static String getAllGroupsTargetFilter(final List<RolloutGroup> groups) {
        if (groups.stream().anyMatch(g -> StringUtils.isEmpty(g.getTargetFilterQuery()))) {
            return "";
        }
        return groups.stream().map(RolloutGroup::getTargetFilterQuery).collect(Collectors.joining(","));
    }

    /**
     * Creates an RSQL Filter that matches all targets that are in the provided group and
     * in the provided groups.
     *
     * @param groups the rollout groups
     * @param group   the group
     * @return RSQL string without base filter of the Rollout. Can be an empty string.
     */
    static String getOverlappingWithGroupsTargetFilter(final List<RolloutGroup> groups, final RolloutGroup group) {
        final String previousGroupFilters = getAllGroupsTargetFilter(groups);
        if (StringUtils.isNotEmpty(previousGroupFilters) && StringUtils.isNotEmpty(group.getTargetFilterQuery())) {
            return group.getTargetFilterQuery() + ";(" + previousGroupFilters + ")";
        } else if (StringUtils.isNotEmpty(previousGroupFilters)) {
            return "(" + previousGroupFilters + ")";
        } else if (StringUtils.isNotEmpty(group.getTargetFilterQuery())) {
            return group.getTargetFilterQuery();
        } else {
            return "";
        }
    }

    static void verifyRemainingTargets(final long targetCount) {
        if (targetCount > 0) {
            throw new RolloutVerificationException(
                    "Rollout groups don't match all targets that are targeted by the rollout");
        }
        if (targetCount != 0) {
            throw new RolloutVerificationException("Rollout groups target count verification failed");
        }
    }

}
