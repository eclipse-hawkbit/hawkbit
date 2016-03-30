/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.eclipse.hawkbit.cache.CacheField;
import org.eclipse.hawkbit.cache.CacheKeys;
import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * @author Michael Hirsch
 *
 */
@Entity
@Table(name = "sp_rollout", indexes = {
        @Index(name = "sp_idx_rollout_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_rollout"))
public class Rollout extends NamedEntity {

    private static final long serialVersionUID = 1L;

    @OneToMany(targetEntity = RolloutGroup.class)
    @JoinColumn(name = "rollout", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollout_rolloutgroup"))
    private List<RolloutGroup> rolloutGroups;

    @Column(name = "target_filter", length = 1024, nullable = false)
    private String targetFilterQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolltout_ds"))
    private DistributionSet distributionSet;

    @Column(name = "status")
    private RolloutStatus status = RolloutStatus.CREATING;

    @Column(name = "last_check")
    private long lastCheck = 0L;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType = ActionType.FORCED;

    @Column(name = "forced_time")
    private long forcedTime;

    @Column(name = "total_targets")
    private long totalTargets;

    @Transient
    @CacheField(key = CacheKeys.ROLLOUT_GROUP_TOTAL)
    private int rolloutGroupsTotal = 0;

    @Transient
    @CacheField(key = CacheKeys.ROLLOUT_GROUP_CREATED)
    private int rolloutGroupsCreated = 0;

    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

    public List<RolloutGroup> getRolloutGroups() {
        return rolloutGroups;
    }

    public void setRolloutGroups(final List<RolloutGroup> rolloutGroups) {
        this.rolloutGroups = rolloutGroups;
    }

    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    public RolloutStatus getStatus() {
        return status;
    }

    public void setStatus(final RolloutStatus status) {
        this.status = status;
    }

    public long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(final long lastCheck) {
        this.lastCheck = lastCheck;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    public long getForcedTime() {
        return forcedTime;
    }

    public void setForcedTime(final long forcedTime) {
        this.forcedTime = forcedTime;
    }

    public long getTotalTargets() {
        return totalTargets;
    }

    public void setTotalTargets(final long totalTargets) {
        this.totalTargets = totalTargets;
    }

    public int getRolloutGroupsTotal() {
        return rolloutGroupsTotal;
    }

    public void setRolloutGroupsTotal(final int rolloutGroupsTotal) {
        this.rolloutGroupsTotal = rolloutGroupsTotal;
    }

    public int getRolloutGroupsCreated() {
        return rolloutGroupsCreated;
    }

    public void setRolloutGroupsCreated(final int rolloutGroupsCreated) {
        this.rolloutGroupsCreated = rolloutGroupsCreated;
    }

    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus(totalTargets);
        }
        return totalTargetCountStatus;
    }

    public void setTotalTargetCountStatus(final TotalTargetCountStatus totalTargetCountStatus) {
        this.totalTargetCountStatus = totalTargetCountStatus;
    }

    @Override
    public String toString() {
        return "Rollout [rolloutGroups=" + rolloutGroups + ", targetFilterQuery=" + targetFilterQuery
                + ", distributionSet=" + distributionSet + ", status=" + status + ", lastCheck=" + lastCheck
                + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    /**
     *
     * State machine for rollout.
     *
     */
    public enum RolloutStatus {

        /**
         * Rollouts is beeing created.
         */
        CREATING,

        /**
         * Rollout is ready to start.
         */
        READY,

        /**
         * Rollout is paused.
         */
        PAUSED,

        /**
         * Rollout is starting.
         */
        STARTING,

        /**
         * Rollout is stopped.
         */
        STOPPED,

        /**
         * Rollout is running.
         */
        RUNNING,

        /**
         * Rollout is finished.
         */
        FINISHED,

        /**
         * Rollout could not created due errors, might be database problem due
         * asynchronous creating.
         */
        ERROR_CREATING,

        /**
         * Rollout could not started due errors, might be database problem due
         * asynchronous starting.
         */
        ERROR_STARTING;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Rollout)) {
            return false;
        }

        return true;
    }
}
