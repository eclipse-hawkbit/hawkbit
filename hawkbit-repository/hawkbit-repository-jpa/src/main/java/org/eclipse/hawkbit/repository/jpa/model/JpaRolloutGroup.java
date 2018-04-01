/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * JPA entity definition of persisting a group of an rollout.
 *
 */
@Entity
@Table(name = "sp_rolloutgroup", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "rollout",
        "tenant" }, name = "uk_rolloutgroup"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaRolloutGroup extends AbstractJpaNamedEntity implements RolloutGroup, EventAwareEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolloutgroup_rollout"))
    private JpaRollout rollout;

    @Column(name = "status", nullable = false)
    @ObjectTypeConverter(name = "rolloutgroupstatus", objectType = RolloutGroup.RolloutGroupStatus.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "READY", dataValue = "0"),
            @ConversionValue(objectValue = "SCHEDULED", dataValue = "1"),
            @ConversionValue(objectValue = "FINISHED", dataValue = "2"),
            @ConversionValue(objectValue = "ERROR", dataValue = "3"),
            @ConversionValue(objectValue = "RUNNING", dataValue = "4"),
            @ConversionValue(objectValue = "CREATING", dataValue = "5") })
    @Convert("rolloutgroupstatus")
    private RolloutGroupStatus status = RolloutGroupStatus.CREATING;

    @CascadeOnDelete
    @OneToMany(mappedBy = "rolloutGroup", fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST }, targetEntity = RolloutTargetGroup.class)
    private List<RolloutTargetGroup> rolloutTargetGroup;

    // No foreign key to avoid to many nested cascades on delete which some DBs cannot handle
    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "parent_id")
    private JpaRolloutGroup parent;

    @Column(name = "success_condition", nullable = false)
    @NotNull
    private RolloutGroupSuccessCondition successCondition = RolloutGroupSuccessCondition.THRESHOLD;

    @Column(name = "success_condition_exp", length = 512, nullable = false)
    @Size(max = 512)
    @NotNull
    private String successConditionExp;

    @Column(name = "success_action", nullable = false)
    @NotNull
    private RolloutGroupSuccessAction successAction = RolloutGroupSuccessAction.NEXTGROUP;

    @Column(name = "success_action_exp", length = 512)
    @Size(max = 512)
    private String successActionExp;

    @Column(name = "error_condition")
    private RolloutGroupErrorCondition errorCondition;

    @Column(name = "error_condition_exp", length = 512)
    @Size(max = 512)
    private String errorConditionExp;

    @Column(name = "error_action")
    private RolloutGroupErrorAction errorAction;

    @Column(name = "error_action_exp", length = 512)
    @Size(max = 512)
    private String errorActionExp;

    @Column(name = "total_targets")
    private int totalTargets;

    @Column(name = "target_filter", length = 1024)
    @Size(max = 1024)
    private String targetFilterQuery = "";

    @Column(name = "target_percentage")
    private float targetPercentage = 100;

    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    @Override
    public Rollout getRollout() {
        return rollout;
    }

    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    @Override
    public RolloutGroupStatus getStatus() {
        return status;
    }

    public void setStatus(final RolloutGroupStatus status) {
        this.status = status;
    }

    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        if (rolloutTargetGroup == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(rolloutTargetGroup);
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

    public void setSuccessCondition(final RolloutGroupSuccessCondition finishCondition) {
        successCondition = finishCondition;
    }

    @Override
    public String getSuccessConditionExp() {
        return successConditionExp;
    }

    public void setSuccessConditionExp(final String finishExp) {
        successConditionExp = finishExp;
    }

    @Override
    public RolloutGroupErrorCondition getErrorCondition() {
        return errorCondition;
    }

    public void setErrorCondition(final RolloutGroupErrorCondition errorCondition) {
        this.errorCondition = errorCondition;
    }

    @Override
    public String getErrorConditionExp() {
        return errorConditionExp;
    }

    public void setErrorConditionExp(final String errorExp) {
        errorConditionExp = errorExp;
    }

    @Override
    public RolloutGroupErrorAction getErrorAction() {
        return errorAction;
    }

    public void setErrorAction(final RolloutGroupErrorAction errorAction) {
        this.errorAction = errorAction;
    }

    @Override
    public String getErrorActionExp() {
        return errorActionExp;
    }

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
    public int getTotalTargets() {
        return totalTargets;
    }

    public void setTotalTargets(final int totalTargets) {
        this.totalTargets = totalTargets;
    }

    public void setSuccessAction(final RolloutGroupSuccessAction successAction) {
        this.successAction = successAction;
    }

    public void setSuccessActionExp(final String successActionExp) {
        this.successActionExp = successActionExp;
    }

    @Override
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    @Override
    public float getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(final float targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    /**
     * @return the totalTargetCountStatus
     */
    @Override
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus(Long.valueOf(totalTargets));
        }
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
        return "RolloutGroup [rollout=" + (rollout != null ? rollout.getId() : "") + ", status=" + status
                + ", rolloutTargetGroup=" + rolloutTargetGroup + ", parent=" + parent + ", finishCondition="
                + successCondition + ", finishExp=" + successConditionExp + ", errorCondition=" + errorCondition
                + ", errorExp=" + errorConditionExp + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        // there is no RolloutGroup created event
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutGroupUpdatedEvent(this,
                this.getRollout().getId(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutGroupDeletedEvent(getTenant(),
                getId(), getClass().getName(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
