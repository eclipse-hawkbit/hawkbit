/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_set", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_ds"))
    @NotNull
    private JpaDistributionSet distributionSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_act_hist_targ"))
    @NotNull
    private JpaTarget target;

    @Column(name = "active")
    private boolean active;

    @Column(name = "action_type", nullable = false)
    @ObjectTypeConverter(name = "actionType", objectType = Action.ActionType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "FORCED", dataValue = "0"),
            @ConversionValue(objectValue = "SOFT", dataValue = "1"),
            @ConversionValue(objectValue = "TIMEFORCED", dataValue = "2"),
            @ConversionValue(objectValue = "DOWNLOAD_ONLY", dataValue = "3") })
    @Convert("actionType")
    @NotNull
    private ActionType actionType;

    @Column(name = "forced_time")
    private long forcedTime;

    @Column(name = "weight")
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    @Column(name = "status", nullable = false)
    @ObjectTypeConverter(name = "status", objectType = Action.Status.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "FINISHED", dataValue = "0"),
            @ConversionValue(objectValue = "ERROR", dataValue = "1"),
            @ConversionValue(objectValue = "WARNING", dataValue = "2"),
            @ConversionValue(objectValue = "RUNNING", dataValue = "3"),
            @ConversionValue(objectValue = "CANCELED", dataValue = "4"),
            @ConversionValue(objectValue = "CANCELING", dataValue = "5"),
            @ConversionValue(objectValue = "RETRIEVED", dataValue = "6"),
            @ConversionValue(objectValue = "DOWNLOAD", dataValue = "7"),
            @ConversionValue(objectValue = "SCHEDULED", dataValue = "8"),
            @ConversionValue(objectValue = "CANCEL_REJECTED", dataValue = "9"),
            @ConversionValue(objectValue = "DOWNLOADED", dataValue = "10"),
            @ConversionValue(objectValue = "WAIT_FOR_CONFIRMATION", dataValue = "11")})
    @Convert("status")
    @NotNull
    private Status status;

    @CascadeOnDelete
    @OneToMany(mappedBy = "action", targetEntity = JpaActionStatus.class, fetch = FetchType.LAZY)
    private List<JpaActionStatus> actionStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rolloutgroup", updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rolloutgroup"))
    private JpaRolloutGroup rolloutGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout", updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rollout"))
    private JpaRollout rollout;

    @Column(name = "maintenance_cron_schedule", updatable = false, length = Action.MAINTENANCE_WINDOW_SCHEDULE_LENGTH)
    private String maintenanceWindowSchedule;

    @Column(name = "maintenance_duration", updatable = false, length = Action.MAINTENANCE_WINDOW_DURATION_LENGTH)
    private String maintenanceWindowDuration;

    @Column(name = "maintenance_time_zone", updatable = false, length = Action.MAINTENANCE_WINDOW_TIMEZONE_LENGTH)
    private String maintenanceWindowTimeZone;

    @Column(name = "external_ref", length = Action.EXTERNAL_REF_MAX_LENGTH)
    private String externalRef;

    @Column(name = "initiated_by", updatable = false, nullable = false, length = USERNAME_FIELD_LENGTH)
    private String initiatedBy;

    @Column(name = "last_action_status_code", nullable = true, updatable = true)
    private Integer lastActionStatusCode;

    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

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

    public List<ActionStatus> getActionStatus() {
        if (actionStatus == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(actionStatus);
    }

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
    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
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
        return "JpaAction [distributionSet=" + distributionSet.getId() + ", version=" + getOptLockRevision() + ", id="
                + getId() + ", actionType=" + getActionType() + ", weight=" + getWeight() + ", isActive=" + isActive()
                + ",  createdAt=" + getCreatedAt() + ", lastModifiedAt=" + getLastModifiedAt() + ", status="
                + getStatus().name() + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new ActionCreatedEvent(this, BaseEntity.getIdOrNull(target),
                        BaseEntity.getIdOrNull(rollout), BaseEntity.getIdOrNull(rolloutGroup),
                        EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new ActionUpdatedEvent(this, BaseEntity.getIdOrNull(target),
                        BaseEntity.getIdOrNull(rollout), BaseEntity.getIdOrNull(rolloutGroup),
                        EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        // there is no action deletion
    }

    @Override
    public String getMaintenanceWindowSchedule() {
        return maintenanceWindowSchedule;
    }

    /**
     * Sets the maintenance schedule.
     *
     * @param maintenanceWindowSchedule
     *            is a cron expression to be used for scheduling.
     */
    public void setMaintenanceWindowSchedule(final String maintenanceWindowSchedule) {
        this.maintenanceWindowSchedule = maintenanceWindowSchedule;
    }

    @Override
    public String getMaintenanceWindowDuration() {
        return maintenanceWindowDuration;
    }

    /**
     * Sets the maintenance window duration.
     *
     * @param maintenanceWindowDuration
     *            is the duration of an available maintenance schedule in
     *            HH:mm:ss format.
     */
    public void setMaintenanceWindowDuration(final String maintenanceWindowDuration) {
        this.maintenanceWindowDuration = maintenanceWindowDuration;
    }

    @Override
    public String getMaintenanceWindowTimeZone() {
        return maintenanceWindowTimeZone;
    }

    /**
     * Sets the time zone to be used for maintenance window.
     *
     * @param maintenanceWindowTimeZone
     *            is the time zone specified as +/-hh:mm offset from UTC for
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     */
    public void setMaintenanceWindowTimeZone(final String maintenanceWindowTimeZone) {
        this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
    }

    @Override
    public Optional<ZonedDateTime> getMaintenanceWindowStartTime() {
        return MaintenanceScheduleHelper.getNextMaintenanceWindow(maintenanceWindowSchedule, maintenanceWindowDuration,
                maintenanceWindowTimeZone);
    }

    /**
     * Returns the end time of next available or active maintenance window for
     * the {@link Action} as {@link ZonedDateTime}. If a maintenance window is
     * already active, the end time of currently active window is returned.
     *
     * @return the end time of window as { @link Optional<ZonedDateTime>}.
     */
    private Optional<ZonedDateTime> getMaintenanceWindowEndTime() {
        return getMaintenanceWindowStartTime()
                .map(start -> start.plus(MaintenanceScheduleHelper.convertToISODuration(maintenanceWindowDuration)));
    }

    @Override
    public boolean hasMaintenanceSchedule() {
        return this.maintenanceWindowSchedule != null;
    }

    @Override
    public boolean isMaintenanceScheduleLapsed() {
        return !getMaintenanceWindowStartTime().isPresent();
    }

    @Override
    public boolean isMaintenanceWindowAvailable() {
        if (!hasMaintenanceSchedule()) {
            // if there is no defined maintenance schedule, a window is always
            // available.
            return true;
        } else if (isMaintenanceScheduleLapsed()) {
            // if a defined maintenance schedule has lapsed, a window is never
            // available.
            return false;
        } else {
            final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.of(maintenanceWindowTimeZone));
            final Optional<ZonedDateTime> start = getMaintenanceWindowStartTime();
            final Optional<ZonedDateTime> end = getMaintenanceWindowEndTime();

            if (start.isPresent() && end.isPresent()) {
                return now.isAfter(start.get()) && now.isBefore(end.get());
            } else {
                return false;
            }
        }
    }

    @Override
    public void setExternalRef(final String externalRef) {
        this.externalRef = externalRef;
    }

    @Override
    public String getExternalRef() {
        return externalRef;
    }

    public void setInitiatedBy(final String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    @Override
    public String getInitiatedBy() {
        return initiatedBy;
    }

    @Override
    public Optional<Integer> getLastActionStatusCode() {
        return Optional.ofNullable(lastActionStatusCode);
    }

    public void setLastActionStatusCode(final Integer lastActionStatusCode) {
        this.lastActionStatusCode = lastActionStatusCode;
    }

    public boolean isWaitingConfirmation() {
        return status == Status.WAIT_FOR_CONFIRMATION;
    }
}
