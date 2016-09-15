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

import org.eclipse.hawkbit.repository.eventbus.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.remote.entity.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityPropertyChangeHelper;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * JPA implementation of {@link Action}.
 */
@Table(name = "sp_action", indexes = { @Index(name = "sp_idx_action_01", columnList = "tenant,distribution_set"),
        @Index(name = "sp_idx_action_02", columnList = "tenant,target,active"),
        @Index(name = "sp_idx_action_prim", columnList = "tenant,id") })
@NamedEntityGraphs({ @NamedEntityGraph(name = "Action.ds", attributeNodes = { @NamedAttributeNode("distributionSet") }),
        @NamedEntityGraph(name = "Action.all", attributeNodes = { @NamedAttributeNode("distributionSet"),
                @NamedAttributeNode(value = "target", subgraph = "target.ds") }, subgraphs = @NamedSubgraph(name = "target.ds", attributeNodes = @NamedAttributeNode("assignedDistributionSet"))) })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaAction extends AbstractJpaTenantAwareBaseEntity implements Action, EventAwareEntity {
    private static final long serialVersionUID = 1L;

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

    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    @Override
    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

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
    public boolean isActive() {
        return active;
    }

    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

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

    public void setForcedTime(final long forcedTime) {
        this.forcedTime = forcedTime;
    }

    @Override
    public RolloutGroup getRolloutGroup() {
        return rolloutGroup;
    }

    public void setRolloutGroup(final RolloutGroup rolloutGroup) {
        this.rolloutGroup = (JpaRolloutGroup) rolloutGroup;
    }

    @Override
    public Rollout getRollout() {
        return rollout;
    }

    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    @Override
    public String toString() {
        return "Action [distributionSet=" + distributionSet + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventBusHolder.getInstance().getApplicationEventPublisher()
                .publishEvent(new ActionCreatedEvent(this, EventBusHolder.getInstance().getNodeId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventBusHolder.getInstance().getApplicationEventPublisher().publishEvent(new ActionPropertyChangeEvent(this,
                EntityPropertyChangeHelper.getChangeSet(descriptorEvent), EventBusHolder.getInstance().getNodeId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        // there is no action deletion
    }

}
