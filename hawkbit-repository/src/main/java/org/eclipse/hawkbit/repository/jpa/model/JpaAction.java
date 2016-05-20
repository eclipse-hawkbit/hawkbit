/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.hawkbit.cache.CacheField;
import org.eclipse.hawkbit.cache.CacheKeys;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * <p>
 * Applicable transition changes of the {@link SoftwareModule}s state of a
 * {@link Target}, e.g. install, uninstall, update and preparations for the
 * transition change, i.e. download.
 * </p>
 *
 * <p>
 * Actions are managed by the SP server and applied to the targets by the
 * client.
 * <p>
 */
@Table(name = "sp_action", indexes = { @Index(name = "sp_idx_action_01", columnList = "tenant,distribution_set"),
        @Index(name = "sp_idx_action_02", columnList = "tenant,target,active"),
        @Index(name = "sp_idx_action_prim", columnList = "tenant,id") })
@NamedEntityGraphs({ @NamedEntityGraph(name = "Action.ds", attributeNodes = { @NamedAttributeNode("distributionSet") }),
        @NamedEntityGraph(name = "Action.all", attributeNodes = { @NamedAttributeNode("distributionSet"),
                @NamedAttributeNode(value = "target", subgraph = "target.ds") }, subgraphs = @NamedSubgraph(name = "target.ds", attributeNodes = @NamedAttributeNode("assignedDistributionSet"))) })
@Entity
public class JpaAction extends JpaTenantAwareBaseEntity implements Action {
    private static final long serialVersionUID = 1L;

    /**
     * the {@link DistributionSet} which should be installed by this action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_ds"))
    private JpaDistributionSet distributionSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_target"))
    private JpaTarget target;

    @Column(name = "active")
    private boolean active;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "forced_time")
    private long forcedTime;

    @Column(name = "status")
    private Status status;

    @CascadeOnDelete
    @OneToMany(mappedBy = "action", targetEntity = JpaActionStatus.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.REMOVE })
    private List<ActionStatus> actionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rolloutgroup", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rolloutgroup"))
    private JpaRolloutGroup rolloutGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rollout"))
    private JpaRollout rollout;

    /**
     * Note: filled only in {@link Status#DOWNLOAD}.
     */
    @Transient
    @CacheField(key = CacheKeys.DOWNLOAD_PROGRESS_PERCENT)
    private int downloadProgressPercent;

    /**
     * @return the distributionSet
     */
    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    /**
     * @param distributionSet
     *            the distributionSet to set
     */
    @Override
    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    /**
     * @return true when action is in state {@link Status#CANCELING} or
     *         {@link Status#CANCELED}, false otherwise
     */
    @Override
    public boolean isCancelingOrCanceled() {
        return status == Status.CANCELING || status == Status.CANCELED;
    }

    @Override
    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public int getDownloadProgressPercent() {
        return downloadProgressPercent;
    }

    @Override
    public void setDownloadProgressPercent(final int downloadProgressPercent) {
        this.downloadProgressPercent = downloadProgressPercent;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * @return the actionType
     */
    @Override
    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public List<ActionStatus> getActionStatus() {
        return actionStatus;
    }

    @Override
    public void setTarget(final Target target) {
        this.target = (JpaTarget) target;
    }

    @Override
    public Target getTarget() {
        return target;
    }

    @Override
    public long getForcedTime() {
        return forcedTime;
    }

    @Override
    public void setForcedTime(final long forcedTime) {
        this.forcedTime = forcedTime;
    }

    @Override
    public RolloutGroup getRolloutGroup() {
        return rolloutGroup;
    }

    @Override
    public void setRolloutGroup(final RolloutGroup rolloutGroup) {
        this.rolloutGroup = (JpaRolloutGroup) rolloutGroup;
    }

    @Override
    public Rollout getRollout() {
        return rollout;
    }

    @Override
    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    /**
     * checks if the {@link #forcedTime} is hit by the given
     * {@code hitTimeMillis}, by means if the given milliseconds are greater
     * than the forcedTime.
     *
     * @param hitTimeMillis
     *            the milliseconds, mostly the
     *            {@link System#currentTimeMillis()}
     * @return {@code true} if this {@link #type} is in
     *         {@link ActionType#TIMEFORCED} and the given {@code hitTimeMillis}
     *         is greater than the {@link #forcedTime} otherwise {@code false}
     */
    @Override
    public boolean isHitAutoForceTime(final long hitTimeMillis) {
        if (actionType == ActionType.TIMEFORCED) {
            return hitTimeMillis >= forcedTime;
        }
        return false;
    }

    /**
     * @return {@code true} if either the {@link #type} is
     *         {@link ActionType#FORCED} or {@link ActionType#TIMEFORCED} but
     *         then if the {@link #forcedTime} has been exceeded otherwise
     *         always {@code false}
     */
    @Override
    public boolean isForce() {
        switch (actionType) {
        case FORCED:
            return true;
        case TIMEFORCED:
            return isHitAutoForceTime(System.currentTimeMillis());
        default:
            return false;
        }
    }

    /**
     * @return true when action is forced, false otherwise
     */
    @Override
    public boolean isForced() {
        return actionType == ActionType.FORCED;
    }

    @Override
    public String toString() {
        return "Action [distributionSet=" + distributionSet + ", getId()=" + getId() + "]";
    }

}
