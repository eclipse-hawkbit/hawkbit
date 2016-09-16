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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.eventbus.event.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.repository.jpa.cache.CacheField;
import org.eclipse.hawkbit.repository.jpa.cache.CacheKeys;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityPropertyChangeHelper;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * JPA implementation of a {@link Rollout}.
 *
 */
@Entity
@Table(name = "sp_rollout", indexes = {
        @Index(name = "sp_idx_rollout_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_rollout"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaRollout extends AbstractJpaNamedEntity implements Rollout, EventAwareEntity {

    private static final long serialVersionUID = 1L;

    @OneToMany(targetEntity = JpaRolloutGroup.class)
    @JoinColumn(name = "rollout", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollout_rolloutgroup"))
    private List<RolloutGroup> rolloutGroups;

    @Column(name = "target_filter", length = 1024, nullable = false)
    @Size(max = 1024)
    @NotNull
    private String targetFilterQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolltout_ds"))
    @NotNull
    private JpaDistributionSet distributionSet;

    @Column(name = "status")
    private RolloutStatus status = RolloutStatus.CREATING;

    @Column(name = "last_check")
    private long lastCheck;

    @Column(name = "action_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    @NotNull
    private ActionType actionType = ActionType.FORCED;

    @Column(name = "forced_time")
    private long forcedTime;

    @Column(name = "total_targets")
    private long totalTargets;

    @Transient
    @CacheField(key = CacheKeys.ROLLOUT_GROUP_TOTAL)
    private int rolloutGroupsTotal;

    @Transient
    @CacheField(key = CacheKeys.ROLLOUT_GROUP_CREATED)
    private int rolloutGroupsCreated;

    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    @Override
    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    @Override
    public List<RolloutGroup> getRolloutGroups() {
        return rolloutGroups;
    }

    public void setRolloutGroups(final List<RolloutGroup> rolloutGroups) {
        this.rolloutGroups = rolloutGroups;
    }

    @Override
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    @Override
    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    @Override
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

    @Override
    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
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

    @Override
    public int getRolloutGroupsCreated() {
        return rolloutGroupsCreated;
    }

    public void setRolloutGroupsCreated(final int rolloutGroupsCreated) {
        this.rolloutGroupsCreated = rolloutGroupsCreated;
    }

    @Override
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

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        // there is no rollout creation event
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventBusHolder.getInstance().getEventBus()
                .post(new RolloutPropertyChangeEvent(this, EntityPropertyChangeHelper.getChangeSet(descriptorEvent)));

    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        // there is no rollout deletion event
    }

}
