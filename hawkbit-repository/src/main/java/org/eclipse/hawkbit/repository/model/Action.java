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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.hawkbit.cache.CacheField;
import org.eclipse.hawkbit.cache.CacheKeys;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * <p>
 * Applicable transition changes of the software {@link SoftwareModule} state of
 * a {@link Target}, e.g. install, uninstall, update, start, stop, and
 * preparations for the transition change, i.e. download.
 * </p>
 *
 * <p>
 * Actions are managed by the SP server (SPS) and applied to the edge controller
 * by the SP controller (SPC). Actions may also be value added commands that are
 * nor directly related to SP, e.g. factory reset.
 * <p>
 *
 *
 *
 *
 *
 */
@Table(name = "sp_action", indexes = { @Index(name = "sp_idx_action_01", columnList = "tenant,distribution_set"),
        @Index(name = "sp_idx_action_02", columnList = "tenant,target,active"),
        @Index(name = "sp_idx_action_prim", columnList = "tenant,id") })
@NamedEntityGraphs({ @NamedEntityGraph(name = "Action.ds", attributeNodes = { @NamedAttributeNode("distributionSet") }),
        @NamedEntityGraph(name = "Action.all", attributeNodes = { @NamedAttributeNode("distributionSet"),
                @NamedAttributeNode("target") }) })
@Entity
public class Action extends BaseEntity implements Comparable<Action> {
    private static final long serialVersionUID = 1L;

    /**
     * indicating that target action has no force time {@link #hasForcedTime()}.
     */
    public static final long NO_FORCE_TIME = 0L;

    /**
     * the {@link DistributionSet} which should be installed by this action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_ds") )
    private DistributionSet distributionSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_target") )
    private Target target;

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
    @OneToMany(mappedBy = "action", targetEntity = ActionStatus.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.REMOVE })
    private List<ActionStatus> actionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rolloutgroup", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rolloutgroup") )
    private RolloutGroup rolloutGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rollout") )
    private Rollout rollout;

    /**
     * Note: filled only in {@link Status#DOWNLOAD}.
     */
    @Transient
    @CacheField(key = CacheKeys.DOWNLOAD_PROGRESS_PERCENT)
    private int downloadProgressPercent;

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
     * @return true when action is in state {@link Status#CANCELING} or
     *         {@link Status#CANCELED}, false otherwise
     */
    public boolean isCancelingOrCanceled() {
        return status == Status.CANCELING || status == Status.CANCELED;
    }

    /**
     * @param active
     *            the active to set
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * @return the downloadProgressPercent
     */
    public int getDownloadProgressPercent() {
        return downloadProgressPercent;
    }

    /**
     * @param downloadProgressPercent
     *            the downloadProgressPercent to set
     */
    public void setDownloadProgressPercent(final int downloadProgressPercent) {
        this.downloadProgressPercent = downloadProgressPercent;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param actionType
     *            the actionType to set
     */
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * @return the actionType
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * @return the actionStatus
     */
    public List<ActionStatus> getActionStatus() {
        return actionStatus;
    }

    /**
     * @param target
     *            the target to set
     */
    public void setTarget(final Target target) {
        this.target = target;
    }

    /**
     * @return the target
     */
    public Target getTarget() {
        return target;
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
     * @return the rolloutGroup
     */
    public RolloutGroup getRolloutGroup() {
        return rolloutGroup;
    }

    /**
     * @param rolloutGroup
     *            the rolloutGroup to set
     */
    public void setRolloutGroup(final RolloutGroup rolloutGroup) {
        this.rolloutGroup = rolloutGroup;
    }

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

    @Override
    public int compareTo(final Action o) {
        if (super.getId() == null || o == null || o.getId() == null) {
            return 0;
        }
        return super.getId().compareTo(o.getId());
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
    public boolean isForced() {
        return actionType == ActionType.FORCED;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Action [distributionSet=" + distributionSet + ", getId()=" + getId() + "]";
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() { // NOSONAR - as this is generated
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((actionType == null) ? 0 : actionType.hashCode());
        result = prime * result + (active ? 1231 : 1237);
        result = prime * result + (int) (forcedTime ^ (forcedTime >>> 32));
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + (isHitAutoForceTime(System.currentTimeMillis()) ? 1231 : 1237);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated

        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Action other = (Action) obj;
        if (actionType != other.actionType) {
            return false;
        }
        if (active != other.active) {
            return false;
        }
        if (forcedTime != other.forcedTime) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        return true;
    }

    /**
     * Action status as reported by the controller.
     *
     * Be aware that JPA is persisting the ordinal number of the enum by means
     * the ordered number in the enum. So don't re-order the enums within the
     * Status enum declaration!
     *
     */
    public enum Status {
        /**
         * Action is finished successfully for this target.
         */
        FINISHED,

        /**
         * Action has failed for this target.
         */
        ERROR,

        /**
         * Action is still running but with warnings.
         */
        WARNING,

        /**
         * Action is still running for this target.
         */
        RUNNING,

        /**
         * Action has been canceled for this target.
         */
        CANCELED,

        /**
         * Action is in canceling state and waiting for controller confirmation.
         */
        CANCELING,

        /**
         * Action has been presented to the target.
         */
        RETRIEVED,

        /**
         * Action needs download by this target which has now started.
         */
        DOWNLOAD,

        /**
         * Action is in waiting state, e.g. the action is scheduled in a rollout
         * but not yet activated.
         */
        SCHEDULED;
    }

    /**
     * The action type for this action relation.
     *
     *
     *
     *
     */
    public enum ActionType {
        FORCED, SOFT, TIMEFORCED;
    }
}
