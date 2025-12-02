/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import static org.eclipse.hawkbit.repository.model.BaseEntity.getIdOrNull;

import java.io.Serial;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * JPA implementation of {@link Action}.
 */
@Table(name = "sp_action")
@NamedEntityGraphs({
        @NamedEntityGraph(name = "Action.all", attributeNodes = {
                @NamedAttributeNode(value = "target", subgraph = "target.ds"),
                @NamedAttributeNode("distributionSet") },
                subgraphs = @NamedSubgraph(
                        name = "target.ds",
                        attributeNodes = @NamedAttributeNode("assignedDistributionSet"))),
        @NamedEntityGraph(name = "Action.ds", attributeNodes = { @NamedAttributeNode("distributionSet") })
})
@Entity
// squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
// java:S1710 - not possible to use without group annotation
@SuppressWarnings({ "squid:S2160", "java:S1710", "java:S1171", "java:S3599" })
public class JpaAction extends AbstractJpaTenantAwareBaseEntity implements Action, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "distribution_set", nullable = false, updatable = false)
    @NotNull
    private JpaDistributionSet distributionSet;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "target", updatable = false)
    @NotNull
    private JpaTarget target;

    @Setter
    @Getter
    @Column(name = "active")
    private boolean active;

    @Setter
    @Getter
    @Column(name = "action_type", nullable = false)
    @Convert(converter = ActionTypeConverter.class)
    @NotNull
    private ActionType actionType;

    @Setter
    @Getter
    @Column(name = "forced_time")
    private long forcedTime;

    @Setter
    @Column(name = "weight")
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    @Setter
    @Getter
    @Column(name = "status", nullable = false)
    @Convert(converter = StatusConverter.class)
    @NotNull
    private Status status;

    @OneToMany(mappedBy = "action", targetEntity = JpaActionStatus.class, fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
    private List<JpaActionStatus> actionStatus = new ArrayList<>();

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rollout_group", updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rollout_group"))
    private JpaRolloutGroup rolloutGroup;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rollout", updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_action_rollout"))
    private JpaRollout rollout;

    // a cron expression to be used for scheduling.
    @Setter
    @Getter
    @Column(name = "maintenance_cron_schedule", updatable = false, length = Action.MAINTENANCE_WINDOW_SCHEDULE_LENGTH)
    private String maintenanceWindowSchedule;

    // the duration of an available maintenance schedule indexes HH:mm:ss format
    @Setter
    @Getter
    @Column(name = "maintenance_duration", updatable = false, length = Action.MAINTENANCE_WINDOW_DURATION_LENGTH)
    private String maintenanceWindowDuration;

    // the time zone specified as +/-hh:mm offset from UTC for example +02:00 for CET summer time and +00:00 for UTC. The
    // start time of a maintenance window calculated based on the cron expression is relative to this time zone.
    @Setter
    @Getter
    @Column(name = "maintenance_time_zone", updatable = false, length = Action.MAINTENANCE_WINDOW_TIMEZONE_LENGTH)
    private String maintenanceWindowTimeZone;

    @Setter
    @Getter
    @Column(name = "external_ref", length = Action.EXTERNAL_REF_MAX_LENGTH)
    private String externalRef;

    @Setter
    @Getter
    @Column(name = "initiated_by", updatable = false, nullable = false, length = USERNAME_FIELD_LENGTH)
    private String initiatedBy;

    @Setter
    @Column(name = "last_action_status_code", nullable = true, updatable = true)
    private Integer lastActionStatusCode;

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    public void setTarget(final Target target) {
        this.target = (JpaTarget) target;
    }

    @Override
    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    public void setRolloutGroup(final RolloutGroup rolloutGroup) {
        this.rolloutGroup = (JpaRolloutGroup) rolloutGroup;
    }

    public void setRollout(final Rollout rollout) {
        this.rollout = (JpaRollout) rollout;
    }

    @Override
    public Optional<Integer> getLastActionStatusCode() {
        return Optional.ofNullable(lastActionStatusCode);
    }

    @Override
    public Optional<ZonedDateTime> getMaintenanceWindowStartTime() {
        return MaintenanceScheduleHelper.getNextMaintenanceWindow(
                maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone);
    }

    @Override
    public boolean hasMaintenanceSchedule() {
        return this.maintenanceWindowSchedule != null;
    }

    @Override
    public boolean isMaintenanceScheduleLapsed() {
        return getMaintenanceWindowStartTime().isEmpty();
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

    public boolean isWaitingConfirmation() {
        return status == Status.WAIT_FOR_CONFIRMATION;
    }

    public List<ActionStatus> getActionStatus() {
        return Collections.unmodifiableList(actionStatus);
    }

    @Override
    public String toString() {
        return "JpaAction [distributionSet=" + distributionSet.getId() + ", version=" + getOptLockRevision() + ", id=" + getId() +
                ", actionType=" + getActionType() + ", weight=" + getWeight() + ", isActive=" + isActive() +
                ",  createdAt=" + getCreatedAt() + ", lastModifiedAt=" + getLastModifiedAt() + ", status=" + getStatus().name() + "]";
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new ActionCreatedEvent(this, getIdOrNull(target), getIdOrNull(rollout), getIdOrNull(rolloutGroup)));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new ActionUpdatedEvent(this, getIdOrNull(target), getIdOrNull(rollout), getIdOrNull(rolloutGroup)));
    }

    @Override
    public void fireDeleteEvent() {
        // there is no action deletion
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

    @Converter
    public static class ActionTypeConverter extends MapAttributeConverter<ActionType, Integer> {

        public ActionTypeConverter() {
            super(Map.of(
                    ActionType.FORCED, 0,
                    ActionType.SOFT, 1,
                    ActionType.TIMEFORCED, 2,
                    ActionType.DOWNLOAD_ONLY, 3
            ), null);
        }
    }

    @Converter
    public static class StatusConverter extends MapAttributeConverter<Status, Integer> {

        public StatusConverter() {
            super(new EnumMap<>(Status.class) {{
                put(Status.FINISHED, 0);
                put(Status.ERROR, 1);
                put(Status.WARNING, 2);
                put(Status.RUNNING, 3);
                put(Status.CANCELED, 4);
                put(Status.CANCELING, 5);
                put(Status.RETRIEVED, 6);
                put(Status.DOWNLOAD, 7);
                put(Status.SCHEDULED, 8);
                put(Status.CANCEL_REJECTED, 9);
                put(Status.DOWNLOADED, 10);
                put(Status.WAIT_FOR_CONFIRMATION, 11);
            }}, null);
        }
    }
}