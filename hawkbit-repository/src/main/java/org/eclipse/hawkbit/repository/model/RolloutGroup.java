/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * @author Michael Hirsch
 *
 */
@Entity
@Table(name = "sp_rolloutgroup", indexes = { @Index(name = "sp_idx_rolloutgroup_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
        "name", "rollout", "tenant" }, name = "uk_rolloutgroup"))
public class RolloutGroup extends NamedEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolloutgroup_rollout"))
    private Rollout rollout;

    @Column(name = "status")
    private RolloutGroupStatus status = RolloutGroupStatus.READY;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "rolloutGroup_Id", insertable = false, updatable = false)
    private final List<RolloutTargetGroup> rolloutTargetGroup = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private RolloutGroup parent;

    @Column(name = "success_condition", nullable = false)
    private RolloutGroupSuccessCondition successCondition = RolloutGroupSuccessCondition.THRESHOLD;

    @Column(name = "success_condition_exp", length = 512, nullable = false)
    private String successConditionExp = null;

    @Column(name = "success_action", nullable = false)
    private final RolloutGroupSuccessAction successAction = RolloutGroupSuccessAction.NEXTGROUP;

    @Column(name = "success_action_exp", length = 512, nullable = false)
    private final String successActionExp = null;

    @Column(name = "error_condition")
    private RolloutGroupErrorCondition errorCondition = null;

    @Column(name = "error_condition_exp", length = 512)
    private String errorConditionExp = null;

    @Column(name = "error_action")
    private RolloutGroupErrorAction errorAction = null;

    @Column(name = "error_action_exp", length = 512)
    private String errorActionExp = null;

    @Transient
    private TotalTargetCountStatus totalTargetCountStatus;

    /**
     * @return the rollout
     */
    public Rollout getRollout() {
        return rollout;
    }

    /**
     * @param rollout
     *            the rollout to set
     */
    public void setRollout(final Rollout rollout) {
        this.rollout = rollout;
    }

    /**
     * @return the status
     */
    public RolloutGroupStatus getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final RolloutGroupStatus status) {
        this.status = status;
    }

    /**
     * @return the rolloutTargetGroup
     */
    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        return rolloutTargetGroup;
    }

    /**
     * @return the parent
     */
    public RolloutGroup getParent() {
        return parent;
    }

    /**
     * @param parent
     *            the parent to set
     */
    public void setParent(final RolloutGroup parent) {
        this.parent = parent;
    }

    /**
     * @return the finishCondition
     */
    public RolloutGroupSuccessCondition getSuccessCondition() {
        return successCondition;
    }

    /**
     * @param finishCondition
     *            the finishCondition to set
     */
    public void setSuccessCondition(final RolloutGroupSuccessCondition finishCondition) {
        this.successCondition = finishCondition;
    }

    /**
     * @return the finishExp
     */
    public String getSuccessConditionExp() {
        return successConditionExp;
    }

    /**
     * @param finishExp
     *            the finishExp to set
     */
    public void setSuccessConditionExp(final String finishExp) {
        this.successConditionExp = finishExp;
    }

    /**
     * @return the errorCondition
     */
    public RolloutGroupErrorCondition getErrorCondition() {
        return errorCondition;
    }

    /**
     * @param errorCondition
     *            the errorCondition to set
     */
    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    /**
     * @return the errorExp
     */
    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    /**
     * @param errorExp
     *            the errorExp to set
     */
    public void setErrorConditionExp(final String errorExp) {
        this.errorConditionExp = errorExp;
    }

    /**
     * @return the errorAction
     */
    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    /**
     * @param errorAction
     *            the errorAction to set
     */
    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    /**
     * @return the errorActionExp
     */
    public String getErrorActionExp() {
        return errorActionExp;
    }

    /**
     * @param errorActionExp
     *            the errorActionExp to set
     */
    public void setErrorActionExp(final String errorActionExp) {
        this.errorActionExp = errorActionExp;
    }

    /**
     * @return the successAction
     */
    public RolloutGroupSuccessAction getSuccessAction() {
        return successAction;
    }

    /**
     * @return the successActionExp
     */
    public String getSuccessActionExp() {
        return successActionExp;
    }

    /**
     * @return the totalTargetCountStatus
     */
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        return totalTargetCountStatus;
    }

    /**
     * @param totalTargetCountStatus
     *            the totalTargetCountStatus to set
     */
    public void setTotalTargetCountStatus(final TotalTargetCountStatus totalTargetCountStatus) {
        this.totalTargetCountStatus = totalTargetCountStatus;
    }

    @Override
    public String toString() {
        return "RolloutGroup [rollout=" + rollout + ", status=" + status + ", rolloutTargetGroup=" + rolloutTargetGroup
                + ", parent=" + parent + ", finishCondition=" + successCondition + ", finishExp=" + successConditionExp
                + ", errorCondition=" + errorCondition + ", errorExp=" + errorConditionExp + ", getName()=" + getName()
                + ", getId()=" + getId() + "]";
    }

    /**
     * 
     * @author Michael Hirsch
     *
     */
    public static enum RolloutGroupStatus {

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

    public static enum RolloutGroupSuccessCondition {
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

    public static enum RolloutGroupErrorCondition {
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
    public static enum RolloutGroupErrorAction {
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
    public static enum RolloutGroupSuccessAction {
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

    /**
     * Object which holds all {@link RolloutGroup} conditions together which can
     * easily built.
     */
    public static class RolloutGroupConditions {
        private RolloutGroupSuccessCondition successCondition = null;
        private String successConditionExp = null;
        private RolloutGroupSuccessAction successAction = null;
        private String successActionExp = null;
        private RolloutGroupErrorCondition errorCondition = null;
        private String errorConditionExp = null;
        private RolloutGroupErrorAction errorAction = null;
        private String errorActionExp = null;

        public RolloutGroupSuccessCondition getSuccessCondition() {
            return successCondition;
        }

        public void setSuccessCondition(final RolloutGroupSuccessCondition finishCondition) {
            this.successCondition = finishCondition;
        }

        public String getSuccessConditionExp() {
            return successConditionExp;
        }

        public void setSuccessConditionExp(final String finishConditionExp) {
            this.successConditionExp = finishConditionExp;
        }

        public RolloutGroupSuccessAction getSuccessAction() {
            return successAction;
        }

        public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
            this.successAction = successAction;
        }

        public String getSuccessActionExp() {
            return successActionExp;
        }

        public void setSuccessActionExp(final String successActionExp) {
            this.successActionExp = successActionExp;
        }

        public RolloutGroupErrorCondition getErrorCondition() {
            return errorCondition;
        }

        public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
            this.errorCondition = errorCondition;
        }

        public String getErrorConditionExp() {
            return errorConditionExp;
        }

        public void setErrorConditionExp(final String errorConditionExp) {
            this.errorConditionExp = errorConditionExp;
        }

        public RolloutGroupErrorAction getErrorAction() {
            return errorAction;
        }

        public void setErrorAction(final RolloutGroupErrorAction errorAction) {
            this.errorAction = errorAction;
        }

        public String getErrorActionExp() {
            return errorActionExp;
        }

        public void setErrorActionExp(final String errorActionExp) {
            this.errorActionExp = errorActionExp;
        }
    }

    /**
     * Builder to build easily the {@link RolloutGroupConditions}.
     *
     */
    public static class RolloutGroupConditionBuilder {
        private final RolloutGroupConditions conditions = new RolloutGroupConditions();

        public RolloutGroupConditions build() {
            return conditions;
        }

        /**
         * Sets the finish condition and expression on the builder.
         * 
         * @param condition
         *            the finish condition
         * @param expression
         *            the finish expression
         * @return the builder itself
         */
        public RolloutGroupConditionBuilder successCondition(final RolloutGroupSuccessCondition condition,
                final String expression) {
            conditions.setSuccessCondition(condition);
            conditions.setSuccessConditionExp(expression);
            return this;
        }

        /**
         * Sets the success action and expression on the builder.
         * 
         * @param action
         *            the success action
         * @param expression
         *            the error expression
         * @return the builder itself
         */
        public RolloutGroupConditionBuilder successAction(final RolloutGroupSuccessAction action,
                final String expression) {
            conditions.setSuccessAction(action);
            conditions.setSuccessActionExp(expression);
            return this;
        }

        /**
         * Sets the error condition and expression on the builder.
         * 
         * @param condition
         *            the error condition
         * @param expression
         *            the error expression
         * @return the builder itself
         */
        public RolloutGroupConditionBuilder errorCondition(final RolloutGroupErrorCondition condition,
                final String expression) {
            conditions.setErrorCondition(condition);
            conditions.setErrorConditionExp(expression);
            return this;
        }

        /**
         * Sets the error action and expression on the builder.
         * 
         * @param action
         *            the error action
         * @param expression
         *            the error expression
         * @return the builder itself
         */
        public RolloutGroupConditionBuilder errorAction(final RolloutGroupErrorAction action, final String expression) {
            conditions.setErrorAction(action);
            conditions.setErrorActionExp(expression);
            return this;
        }
    }

}
