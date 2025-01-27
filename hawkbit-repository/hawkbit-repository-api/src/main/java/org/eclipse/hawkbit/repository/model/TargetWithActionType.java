/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * A custom view on {@link Target} with {@link ActionType}.
 */
@Data
public class TargetWithActionType {

    private final String controllerId;
    private final ActionType actionType;
    private final long forceTime;
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private final Integer weight;
    private final boolean confirmationRequired;
    private final String maintenanceSchedule;
    private final String maintenanceWindowDuration;
    private final String maintenanceWindowTimeZone;

    /**
     * Constructor that uses {@link ActionType#FORCED}
     *
     * @param controllerId ID if the controller
     */
    public TargetWithActionType(final String controllerId) {
        this(controllerId, ActionType.FORCED, 0, null, false);
    }

    /**
     * Constructor that leaves the maintenance info empty
     *
     * @param controllerId for which the action is created.
     * @param actionType specified for the action.
     * @param forceTime after that point in time the action is exposed as forced in case
     *         the type is {@link ActionType#TIMEFORCED}
     * @param weight the priority of an {@link Action}
     * @param confirmationRequired sets the confirmation required flag when starting the
     *         {@link Action}
     */
    public TargetWithActionType(
            final String controllerId, final ActionType actionType, final long forceTime,
            final Integer weight, final boolean confirmationRequired) {
        this(controllerId, actionType, forceTime, weight, null, null, null, confirmationRequired);
    }

    /**
     * Constructor that also accepts maintenance schedule parameters and checks
     * for validity of the specified maintenance schedule.
     *
     * @param controllerId for which the action is created.
     * @param actionType specified for the action.
     * @param forceTime after that point in time the action is exposed as forced in
     *         case the type is {@link ActionType#TIMEFORCED}
     * @param weight the priority of an {@link Action}
     * @param maintenanceSchedule is the cron expression to be used for scheduling maintenance
     *         windows. Expression has 6 mandatory fields and 1 last optional
     *         field: "second minute hour dayofmonth month weekday year"
     * @param maintenanceWindowDuration in HH:mm:ss format specifying the duration of a maintenance
     *         window, for example 00:30:00 for 30 minutes
     * @param maintenanceWindowTimeZone is the time zone specified as +/-hh:mm offset from UTC, for
     *         example +02:00 for CET summer time and +00:00 for UTC. The
     *         start time of a maintenance window calculated based on the
     *         cron expression is relative to this time zone.
     * @throws InvalidMaintenanceScheduleException if the parameters do not define a valid maintenance schedule.
     */
    @SuppressWarnings("java:S107")
    public TargetWithActionType(
            final String controllerId, final ActionType actionType, final long forceTime, final Integer weight,
            final String maintenanceSchedule, final String maintenanceWindowDuration, final String maintenanceWindowTimeZone,
            final boolean confirmationRequired) {
        this.controllerId = controllerId;
        this.actionType = actionType != null ? actionType : ActionType.FORCED;
        this.forceTime = actionType == ActionType.TIMEFORCED ?
                forceTime : RepositoryModelConstants.NO_FORCE_TIME;
        this.weight = weight;
        this.confirmationRequired = confirmationRequired;

        this.maintenanceSchedule = maintenanceSchedule;
        this.maintenanceWindowDuration = maintenanceWindowDuration;
        this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
    }
}