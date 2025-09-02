/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import jakarta.validation.Valid;

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * A custom view on assigning a {@link DistributionSet} to a {@link Target}.
 */
@Data
public class DeploymentRequest {

    private final Long distributionSetId;
    @Valid
    private final TargetWithActionType targetWithActionType;

    /**
     * Constructor that also accepts maintenance schedule parameters and checks for validity of the specified maintenance schedule.
     *
     * @param controllerId for which the action is created.
     * @param distributionSetId of the distribution set that that should be assigned to the controller.
     * @param actionType specified for the action.
     * @param forceTime at what time the type soft turns into forced.
     * @param weight the priority of an {@link Action}.
     * @param maintenanceSchedule is the cron expression to be used for scheduling maintenance windows. Expression has 6 mandatory
     *         fields and 1 last optional field: "second minute hour dayofmonth month weekday year"
     * @param maintenanceWindowDuration in HH:mm:ss format specifying the duration of a maintenance window, for example 00:30:00 for 30 minutes
     * @param maintenanceWindowTimeZone is the time zone specified as +/-hh:mm offset from UTC, for example +02:00 for CET summer time
     *         and +00:00 for UTC. The start time of a maintenance window calculated based on the cron expression is relative to this time zone.
     * @param confirmationRequired is a flag whether the confirmation should be required for the resulting {@link Action} or not. In case the
     *         confirmation is not required, the action will be automatically confirmed and put in the
     *         {@link org.eclipse.hawkbit.repository.model.Action.Status#RUNNING} state. Otherwise, the confirmation flow will be triggered
     *         and the {@link Action} will stay in the {@link org.eclipse.hawkbit.repository.model.Action.Status#WAIT_FOR_CONFIRMATION}
     *         state until the confirmation is given. (Only considered with CONFIRMATION_FLOW active via tenant configuration)
     * @throws InvalidMaintenanceScheduleException if the parameters do not define a valid maintenance schedule.
     */
    @SuppressWarnings("java:S107")
    public DeploymentRequest(
            final String controllerId, final Long distributionSetId, final ActionType actionType, final long forceTime, final Integer weight,
            final String maintenanceSchedule, final String maintenanceWindowDuration, final String maintenanceWindowTimeZone,
            final boolean confirmationRequired) {
        this.targetWithActionType = new TargetWithActionType(
                controllerId, actionType, forceTime, weight,
                maintenanceSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone,
                confirmationRequired);
        this.distributionSetId = distributionSetId;
    }

    public String getControllerId() {
        return targetWithActionType.getControllerId();
    }

    public static DeploymentRequestBuilder builder(final String controllerId, final long distributionSetId) {
        return new DeploymentRequestBuilder(controllerId, distributionSetId);
    }

    @Accessors(fluent = true)
    public static class DeploymentRequestBuilder {

        private final String controllerId;
        private final Long distributionSetId;
        @Setter
        private Integer weight;
        @Setter
        private long forceTime = RepositoryModelConstants.NO_FORCE_TIME;
        @Setter
        private ActionType actionType = ActionType.FORCED;
        private String maintenanceSchedule;
        private String maintenanceWindowDuration;
        private String maintenanceWindowTimeZone;
        @Setter
        private boolean confirmationRequired;

        /**
         * Create a builder for a target distribution set assignment with the
         * mandatory fields
         *
         * @param controllerId ID of the target
         * @param distributionSetId ID of the distribution set
         */
        private DeploymentRequestBuilder(final String controllerId, final Long distributionSetId) {
            this.controllerId = controllerId;
            this.distributionSetId = distributionSetId;
        }

        /**
         * Set a maintenanceWindow
         *
         * @param maintenanceSchedule is the cron expression to be used for scheduling maintenance windows. Expression has 6 mandatory fields
         *         and 1 last optional field: "second minute hour dayofmonth month weekday year"
         * @param maintenanceWindowDuration in HH:mm:ss format specifying the duration of a maintenance window, for example 00:30:00 for 30 minutes
         * @param maintenanceWindowTimeZone is the time zone specified as +/-hh:mm offset from UTC, for example +02:00 for CET summer time
         *         and +00:00 for UTC. The start time of a maintenance window calculated based on the cron expression is relative to this time zone.
         * @return builder
         */
        public DeploymentRequestBuilder maintenance(
                final String maintenanceSchedule, final String maintenanceWindowDuration, final String maintenanceWindowTimeZone) {
            this.maintenanceSchedule = maintenanceSchedule;
            this.maintenanceWindowDuration = maintenanceWindowDuration;
            this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
            return this;
        }

        public DeploymentRequest build() {
            return new DeploymentRequest(controllerId, distributionSetId, actionType, forceTime, weight,
                    maintenanceSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone, confirmationRequired);
        }
    }
}