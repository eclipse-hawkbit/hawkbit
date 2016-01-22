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

import javax.persistence.CascadeType;
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

import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * @author Michael Hirsch
 *
 */
@Entity
@Table(name = "sp_rollout", indexes = { @Index(name = "sp_idx_rollout_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
        "name", "tenant" }, name = "uk_rollout"))
public class Rollout extends NamedEntity {

    private static final long serialVersionUID = 1L;

    @OneToMany(cascade = { CascadeType.ALL }, targetEntity = RolloutGroup.class)
    @JoinColumn(name = "rollout", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollout_rolloutgroup"))
    private List<RolloutGroup> rolloutGroups;

    @Column(name = "target_filter", length = 1024, nullable = false)
    private String targetFilterQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolltout_ds"))
    private DistributionSet distributionSet;

    @Column(name = "status")
    private RolloutStatus status = RolloutStatus.READY;

    @Column(name = "last_check")
    private long lastCheck = 0L;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType = ActionType.FORCED;

    @Column(name = "forced_time")
    private long forcedTime;
    
    @Transient
    private boolean isNew = false;


    /**
     * @return the distributionSet
     */
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    /**
     * @param distributionSet
     *            the distributionSet to set
     */
    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = distributionSet;
    }

    /**
     * @return the rolloutGroups
     */
    public List<RolloutGroup> getRolloutGroups() {
        return rolloutGroups;
    }

    /**
     * @param rolloutGroups
     *            the rolloutGroups to set
     */
    public void setRolloutGroups(final List<RolloutGroup> rolloutGroups) {
        this.rolloutGroups = rolloutGroups;
    }

    /**
     * @return the targetFilterQuery
     */
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    /**
     * @param targetFilterQuery
     *            the targetFilterQuery to set
     */
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    /**
     * @return the status
     */
    public RolloutStatus getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final RolloutStatus status) {
        this.status = status;
    }

    /**
     * @return the lastCheck
     */
    public long getLastCheck() {
        return lastCheck;
    }

    /**
     * @param lastCheck
     *            the lastCheck to set
     */
    public void setLastCheck(final long lastCheck) {
        this.lastCheck = lastCheck;
    }

    /**
     * @return the actionType
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * @param actionType
     *            the actionType to set
     */
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * @return the forcedTime
     */
    public long getForcedTime() {
        return forcedTime;
    }

    /**
     * @param forcedTime
     *            the forcedTime to set
     */
    public void setForcedTime(final long forcedTime) {
        this.forcedTime = forcedTime;
    }
    /**
     * @return the isNew
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * @param isNew
     *            the isNew to set
     */
    public void setNew(final boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public String toString() {
        return "Rollout [rolloutGroups=" + rolloutGroups + ", targetFilterQuery=" + targetFilterQuery
                + ", distributionSet=" + distributionSet + ", status=" + status + ", lastCheck=" + lastCheck
                + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    /**
     * 
     * @author Michael Hirsch
     *
     */
    public static enum RolloutStatus {

        /**
         * Rollout is ready to start.
         */
        READY,

        /**
         * Rollout is paused.
         */
        PAUSED,

        /**
         * Rollout is running.
         */
        RUNNING,

        /**
         * Rollout is running.
         */
        STOPPED,

        /**
         * Rollout is finished.
         */
        FINISHED;
    }
}
