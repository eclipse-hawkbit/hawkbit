/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * The core functionality of a {@link Rollout} is the cascading processing of
 * (sub) deployment groups. The group defines under which conditions the
 * following group is processed.
 *
 */
public interface RolloutGroup extends NamedEntity {

    /**
     * @return the corresponding {@link Rollout} of this group
     */
    Rollout getRollout();

    /**
     * @param rollout
     *            sets the {@link Rollout} for this group
     */
    void setRollout(Rollout rollout);

    /**
     * @return the current {@link RolloutGroupStatus} for this group
     */
    RolloutGroupStatus getStatus();

    /**
     * @param status
     *            the {@link RolloutGroupStatus} to set for this group
     */
    void setStatus(RolloutGroupStatus status);

    /**
     * @return the parent group of this group, in case the group is the root
     *         group it does not have a parent and so return {@code null}
     */
    RolloutGroup getParent();

    /**
     * @return the {@link RolloutGroupSuccessCondition} for this group to
     *         indicate when a group is successful
     */
    RolloutGroupSuccessCondition getSuccessCondition();

    /**
     * @param successCondition
     *            the {@link RolloutGroupSuccessCondition} to be set for this
     *            group to indicate when a group is successfully and a next
     *            group might be started
     */
    void setSuccessCondition(RolloutGroupSuccessCondition successCondition);

    /**
     * @return a String representation of the expression to be evaluated by the
     *         {@link RolloutGroupSuccessCondition} to indicate if the condition
     *         is true, might be {@code null} if no expression must be set for
     *         the {@link RolloutGroupSuccessCondition}
     */
    String getSuccessConditionExp();

    /**
     * @param successConditionExp
     *            sets a String represented expression which is evaluated by the
     *            {@link RolloutGroupSuccessCondition}, might be {@code null} if
     *            the set {@link RolloutGroupSuccessCondition} can handle
     *            {@code null} value
     */
    void setSuccessConditionExp(String successConditionExp);

    /**
     * @return the {@link RolloutGroupErrorCondition} for this group to indicate
     *         when a group should marked as failed
     */
    RolloutGroupErrorCondition getErrorCondition();

    /**
     * 
     * @param errorCondition
     *            the {@link RolloutGroupErrorCondition} to be set for this
     *            group to indicate when a group is marked as failed and the
     *            corresponding {@link RolloutGroupErrorAction} should be
     *            executed
     */
    void setErrorCondition(RolloutGroupErrorCondition errorCondition);

    /**
     * @return a String representation of the expression to be evaluated by the
     *         {@link RolloutGroupErrorCondition} to indicate if the condition
     *         is true, might be {@code null} if no expression must be set for
     *         the {@link RolloutGroupErrorCondition}
     */
    String getErrorConditionExp();

    /**
     * @param errorExp
     *            sets a String represented expression which is evaluated by the
     *            {@link RolloutGroupErrorCondition}, might be {@code null} if
     *            the set {@link RolloutGroupErrorCondition} can handle
     *            {@code null} value
     */
    void setErrorConditionExp(String errorExp);

    /**
     * @return a {@link RolloutGroupErrorAction} which is executed when the
     *         given {@link RolloutGroupErrorCondition} is met, might be
     *         {@code null} if no error action is set
     */
    RolloutGroupErrorAction getErrorAction();

    /**
     * @param errorAction
     *            the {@link RolloutGroupErrorAction} to be set which should be
     *            executed if the {@link RolloutGroupErrorCondition} is met,
     *            might be {@code null} if no error action should be executed
     */
    void setErrorAction(RolloutGroupErrorAction errorAction);

    /**
     * @return a String representation of the expression to be evaluated by the
     *         {@link RolloutGroupErrorAction} might be {@code null} if no
     *         expression must be set for the {@link RolloutGroupErrorAction}
     */
    String getErrorActionExp();

    /**
     * @param errorActionExp
     *            sets a String represented expression which is evaluated by the
     *            {@link RolloutGroupErrorAction}, might be {@code null} if the
     *            set {@link RolloutGroupErrorAction} can handle {@code null}
     *            value
     */
    void setErrorActionExp(String errorActionExp);

    /**
     * @return the {@link RolloutGroupSuccessAction} which is executed if the
     *         {@link RolloutGroupSuccessCondition} is met
     */
    RolloutGroupSuccessAction getSuccessAction();

    /**
     * @return a String representation of the expression to be evaluated by the
     *         {@link RolloutGroupSuccessAction} might be {@code null} if no
     *         expression must be set for the {@link RolloutGroupSuccessAction}
     */
    String getSuccessActionExp();

    /**
     * @param successAction
     *            which is executed if the {@link RolloutGroupSuccessCondition}
     *            is met
     */
    void setSuccessAction(RolloutGroupSuccessAction successAction);

    /**
     *
     * @param successActionExp
     *            a String representation of the expression to be evaluated by
     *            the {@link RolloutGroupSuccessAction} might be {@code null} if
     *            no expression must be set for the
     *            {@link RolloutGroupSuccessAction}
     */
    void setSuccessActionExp(String successActionExp);

    /**
     * @return the total amount of targets containing in this group
     */
    int getTotalTargets();

    /**
     * @return the totalTargetCountStatus
     */
    TotalTargetCountStatus getTotalTargetCountStatus();

    /**
     * @param totalTargetCountStatus
     *            the totalTargetCountStatus to set
     */
    void setTotalTargetCountStatus(TotalTargetCountStatus totalTargetCountStatus);

    /**
     * @return the target filter query, that is used to assign Targets to this
     *         Group
     */
    String getTargetFilterQuery();

    /**
     * @param targetFilterQuery
     *            the target filter query, that is used to assign Targets to
     *            this Group. Can be null, if no restrictions should apply.
     */
    void setTargetFilterQuery(String targetFilterQuery);

    /**
     * @return the percentage of matching Targets that should be assigned to
     *         this Group
     */
    float getTargetPercentage();

    /**
     * @param targetPercentage
     *            the percentage of matching Targets that should be assigned to
     *            this Group
     */
    void setTargetPercentage(float targetPercentage);

    /**
     * Rollout group state machine.
     *
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
     * The condition to evaluate if an group is success state.
     */
    enum RolloutGroupSuccessCondition {
        THRESHOLD("thresholdRolloutGroupSuccessCondition");

        private final String beanName;

        RolloutGroupSuccessCondition(final String beanName) {
            this.beanName = beanName;
        }

        /**
         * @return the beanName
         */
        public String getBeanName() {
            return beanName;
        }
    }

    /**
     * The condition to evaluate if an group is in error state.
     */
    enum RolloutGroupErrorCondition {
        THRESHOLD("thresholdRolloutGroupErrorCondition");

        private final String beanName;

        RolloutGroupErrorCondition(final String beanName) {
            this.beanName = beanName;
        }

        /**
         * @return the beanName
         */
        public String getBeanName() {
            return beanName;
        }
    }

    /**
     * The actions executed when the {@link RolloutGroup#getErrorCondition()} is hit.
     */
    enum RolloutGroupErrorAction {
        PAUSE("pauseRolloutGroupAction");

        private final String beanName;

        RolloutGroupErrorAction(final String beanName) {
            this.beanName = beanName;
        }

        /**
         * @return the beanName
         */
        public String getBeanName() {
            return beanName;
        }
    }

    /**
     * The actions executed when the {@link RolloutGroup#getSuccessCondition()} is
     * hit.
     */
    enum RolloutGroupSuccessAction {
        NEXTGROUP("startNextRolloutGroupAction");

        private final String beanName;

        RolloutGroupSuccessAction(final String beanName) {
            this.beanName = beanName;
        }

        /**
         * @return the beanName
         */
        public String getBeanName() {
            return beanName;
        }
    }
}
