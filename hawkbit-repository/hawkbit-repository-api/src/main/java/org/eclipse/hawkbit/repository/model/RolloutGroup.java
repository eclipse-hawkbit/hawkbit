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

    Rollout getRollout();

    void setRollout(Rollout rollout);

    RolloutGroupStatus getStatus();

    void setStatus(RolloutGroupStatus status);

    RolloutGroup getParent();

    RolloutGroupSuccessCondition getSuccessCondition();

    void setSuccessCondition(RolloutGroupSuccessCondition finishCondition);

    String getSuccessConditionExp();

    void setSuccessConditionExp(String finishExp);

    RolloutGroupErrorCondition getErrorCondition();

    void setErrorCondition(RolloutGroupErrorCondition errorCondition);

    String getErrorConditionExp();

    void setErrorConditionExp(String errorExp);

    RolloutGroupErrorAction getErrorAction();

    void setErrorAction(RolloutGroupErrorAction errorAction);

    String getErrorActionExp();

    void setErrorActionExp(String errorActionExp);

    RolloutGroupSuccessAction getSuccessAction();

    String getSuccessActionExp();

    long getTotalTargets();

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
     * Rollout goup state machine.
     *
     */
    public enum RolloutGroupStatus {

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
        RUNNING;
    }

    /**
     * The condition to evaluate if an group is success state.
     */
    public enum RolloutGroupSuccessCondition {
        THRESHOLD("thresholdRolloutGroupSuccessCondition");

        private final String beanName;

        private RolloutGroupSuccessCondition(final String beanName) {
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
    public enum RolloutGroupErrorCondition {
        THRESHOLD("thresholdRolloutGroupErrorCondition");

        private final String beanName;

        private RolloutGroupErrorCondition(final String beanName) {
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
     * The actions executed when the {@link RolloutGroup#errorCondition} is hit.
     */
    public enum RolloutGroupErrorAction {
        PAUSE("pauseRolloutGroupAction");

        private final String beanName;

        private RolloutGroupErrorAction(final String beanName) {
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
     * The actions executed when the {@link RolloutGroup#successCondition} is
     * hit.
     */
    public enum RolloutGroupSuccessAction {
        NEXTGROUP("startNextRolloutGroupAction");

        private final String beanName;

        private RolloutGroupSuccessAction(final String beanName) {
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
