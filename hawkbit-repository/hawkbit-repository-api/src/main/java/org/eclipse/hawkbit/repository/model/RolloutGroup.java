/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * The core functionality of a {@link Rollout} is the cascading processing of (sub) deployment groups. The group defines under which conditions
 * the following group is processed.
 */
public interface RolloutGroup extends NamedEntity {

    /**
     * @return the corresponding {@link Rollout} of this group
     */
    Rollout getRollout();

    /**
     * @return the current {@link RolloutGroupStatus} for this group
     */
    RolloutGroupStatus getStatus();

    /**
     * @return the parent group of this group, in case the group is the root group it does not have a parent and so return {@code null}
     */
    RolloutGroup getParent();

    /**
     * @return if the group is dynamic
     */
    boolean isDynamic();

    /**
     * @return the {@link RolloutGroupSuccessCondition} for this group to indicate when a group is successful
     */
    RolloutGroupSuccessCondition getSuccessCondition();

    /**
     * @return a String representation of the expression to be evaluated by the {@link RolloutGroupSuccessCondition} to indicate if the
     *         condition is true, might be {@code null} if no expression must be set for the {@link RolloutGroupSuccessCondition}
     */
    String getSuccessConditionExp();

    /**
     * @return the {@link RolloutGroupErrorCondition} for this group to indicate when a group should be marked as failed
     */
    RolloutGroupErrorCondition getErrorCondition();

    /**
     * @return a String representation of the expression to be evaluated by the {@link RolloutGroupErrorCondition} to indicate if the condition
     *         is true, might be {@code null} if no expression must be set for the {@link RolloutGroupErrorCondition}
     */
    String getErrorConditionExp();

    /**
     * @return a {@link RolloutGroupErrorAction} which is executed when the given {@link RolloutGroupErrorCondition} is met, might be
     *         {@code null} if no error action is set
     */
    RolloutGroupErrorAction getErrorAction();

    /**
     * @return a String representation of the expression to be evaluated by the {@link RolloutGroupErrorAction} might be {@code null} if no
     *         expression must be set for the {@link RolloutGroupErrorAction}
     */
    String getErrorActionExp();

    /**
     * @return the {@link RolloutGroupSuccessAction} which is executed if the {@link RolloutGroupSuccessCondition} is met
     */
    RolloutGroupSuccessAction getSuccessAction();

    /**
     * @return a String representation of the expression to be evaluated by the {@link RolloutGroupSuccessAction} might be {@code null} if no
     *         expression must be set for the {@link RolloutGroupSuccessAction}
     */
    String getSuccessActionExp();

    /**
     * @return the total amount of targets containing in this group
     */
    int getTotalTargets();

    /**
     * @return the totalTargetCountStatus
     */
    TotalTargetCountStatus getTotalTargetCountStatus();

    /**
     * @return the target filter query, that is used to assign Targets to this Group
     */
    String getTargetFilterQuery();

    /**
     * @return the percentage of matching Targets that should be assigned to this Group
     */
    float getTargetPercentage();

    /**
     * @return if a confirmation is required for the resulting actions (considered with confirmation flow active only)
     */
    boolean isConfirmationRequired();

    /**
     * Rollout group state machine.
     */
    enum RolloutGroupStatus {

        /**
         * Group has been defined, but not all targets have been assigned yet.
         */
        CREATING,

        /**
         * Ready to start the group.
         */
        READY,

        /**
         * Group is scheduled and started sometime, e.g. trigger of group
         * before.
         */
        SCHEDULED,

        /**
         * Group is finished.
         */
        FINISHED,

        /**
         * Group is finished and has errors.
         */
        ERROR,

        /**
         * Group is running.
         */
        RUNNING
    }

    /**
     * The condition to evaluate if a group is success state.
     */
    enum RolloutGroupSuccessCondition {
        THRESHOLD
    }

    /**
     * The condition to evaluate if a group is in error state.
     */
    enum RolloutGroupErrorCondition {
        THRESHOLD
    }

    /**
     * The actions executed when the {@link RolloutGroup#getErrorCondition()} is
     * hit.
     */
    enum RolloutGroupErrorAction {
        PAUSE
    }

    /**
     * The actions executed when the {@link RolloutGroup#getSuccessCondition()}
     * is hit.
     */
    enum RolloutGroupSuccessAction {
        NEXTGROUP,
        PAUSE
    }
}