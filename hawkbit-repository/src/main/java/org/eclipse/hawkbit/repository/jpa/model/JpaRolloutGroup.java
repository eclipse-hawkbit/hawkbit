/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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

import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;

/**
 * JPA entity definition of persisting a group of an rollout.
 *
 * @author Michael Hirsch
 *
 */
@Entity
@Table(name = "sp_rolloutgroup", indexes = {
        @Index(name = "sp_idx_rolloutgroup_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "rollout", "tenant" }, name = "uk_rolloutgroup"))
public class JpaRolloutGroup extends JpaNamedEntity implements RolloutGroup {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolloutgroup_rollout"))
    private JpaRollout rollout;

    @Column(name = "status")
    private RolloutGroupStatus status = RolloutGroupStatus.READY;

    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST }, targetEntity = RolloutTargetGroup.class)
    @JoinColumn(name = "rolloutGroup_Id", insertable = false, updatable = false)
    private final List<RolloutTargetGroup> rolloutTargetGroup = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private JpaRolloutGroup parent;

    @Column(name = "success_condition", nullable = false)
    private RolloutGroupSuccessCondition successCondition = RolloutGroupSuccessCondition.THRESHOLD;

    @Column(name = "success_condition_exp", length = 512, nullable = false)
    private String successConditionExp = null;

    @Column(name = "success_action", nullable = false)
    private RolloutGroupSuccessAction successAction = RolloutGroupSuccessAction.NEXTGROUP;

    @Column(name = "success_action_exp", length = 512, nullable = false)
    private String successActionExp = null;

    @Column(name = "error_condition")
    private RolloutGroupErrorCondition errorCondition = null;

    @Column(name = "error_condition_exp", length = 512)
    private String errorConditionExp = null;

    @Column(name = "error_action")
    private RolloutGroupErrorAction errorAction = null;

    @Column(name = "error_action_exp", length = 512)
    private String errorActionExp = null;

    @Column(name = "total_targets")
    private long totalTargets;

    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    @Override
    public Rollout getRollout() {
        return rollout;
    }

    @Override
    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    @Override
    public RolloutGroupStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(final RolloutGroupStatus status) {
        this.status = status;
    }

    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        return rolloutTargetGroup;
    }

    @Override
    public RolloutGroup getParent() {
        return parent;
    }

    @Override
    public void setParent(final RolloutGroup parent) {
        this.parent = (JpaRolloutGroup) parent;
    }

    @Override
    public RolloutGroupSuccessCondition getSuccessCondition() {
        return successCondition;
    }

    @Override
    public void setSuccessCondition(final RolloutGroupSuccessCondition finishCondition) {
        successCondition = finishCondition;
    }

    @Override
    public String getSuccessConditionExp() {
        return successConditionExp;
    }

    @Override
    public void setSuccessConditionExp(final String finishExp) {
        successConditionExp = finishExp;
    }

    @Override
    public RolloutGroupErrorCondition getErrorCondition() {
        return errorCondition;
    }

    @Override
    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    @Override
    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    @Override
    public void setErrorConditionExp(final String errorExp) {
        errorConditionExp = errorExp;
    }

    @Override
    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    @Override
    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    @Override
    public String getErrorActionExp() {
        return errorActionExp;
    }

    @Override
    public void setErrorActionExp(final String errorActionExp) {
        this.errorActionExp = errorActionExp;
    }

    @Override
    public RolloutGroupSuccessAction getSuccessAction() {
        return successAction;
    }

    @Override
    public String getSuccessActionExp() {
        return successActionExp;
    }

    @Override
    public long getTotalTargets() {
        return totalTargets;
    }

    @Override
    public void setTotalTargets(final long totalTargets) {
        this.totalTargets = totalTargets;
    }

    @Override
    public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
        this.successAction = successAction;
    }

    @Override
    public void setSuccessActionExp(final String successActionExp) {
        this.successActionExp = successActionExp;
    }

    /**
     * @return the totalTargetCountStatus
     */
    @Override
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus(totalTargets);
        }
        return totalTargetCountStatus;
    }

    /**
     * @param totalTargetCountStatus
     *            the totalTargetCountStatus to set
     */
    @Override
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
            successCondition = finishCondition;
        }

        public String getSuccessConditionExp() {
            return successConditionExp;
        }

        public void setSuccessConditionExp(final String finishConditionExp) {
            successConditionExp = finishConditionExp;
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
