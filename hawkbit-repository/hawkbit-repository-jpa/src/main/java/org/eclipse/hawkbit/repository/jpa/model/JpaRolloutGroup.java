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

import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityPropertyChangeHelper;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventBusHolder;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * JPA entity definition of persisting a group of an rollout.
 *
 */
@Entity
@Table(name = "sp_rolloutgroup", indexes = {
        @Index(name = "sp_idx_rolloutgroup_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "rollout", "tenant" }, name = "uk_rolloutgroup"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaRolloutGroup extends AbstractJpaNamedEntity implements RolloutGroup,EventAwareEntity<JpaRolloutGroup> {

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
    private String successConditionExp;

    @Column(name = "success_action", nullable = false)
    private RolloutGroupSuccessAction successAction = RolloutGroupSuccessAction.NEXTGROUP;

    @Column(name = "success_action_exp", length = 512, nullable = false)
    private String successActionExp;

    @Column(name = "error_condition")
    private RolloutGroupErrorCondition errorCondition;

    @Column(name = "error_condition_exp", length = 512)
    private String errorConditionExp;

    @Column(name = "error_action")
    private RolloutGroupErrorAction errorAction;

    @Column(name = "error_action_exp", length = 512)
    private String errorActionExp;

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

    public void setTotalTargets(final long totalTargets) {
        this.totalTargets = totalTargets;
    }

    public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
        this.successAction = successAction;
    }

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

    @Override
    public void fireCreateEvent(final JpaRolloutGroup jpaRolloutGroup, final DescriptorEvent descriptorEvent) {
        
    }

    @Override
    public void fireUpdateEvent(final JpaRolloutGroup jpaRolloutGroup, final DescriptorEvent descriptorEvent) {
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() -> EventBusHolder.getInstance().getEventBus().
                post(new RolloutGroupPropertyChangeEvent(jpaRolloutGroup,
                EntityPropertyChangeHelper.getChangeSet(RolloutGroup.class, descriptorEvent))));
    }

    @Override
    public void fireDeleteEvent(final JpaRolloutGroup jpaRolloutGroup, final DescriptorEvent descriptorEvent) {
        
    }

}
