/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * Builder for {@link DeploymentRequest}
 *
 */
public class DeploymentRequestBuilder {

    private final String controllerId;
    private final Long distributionSetId;
    private Integer weight;
    private long forceTime = RepositoryModelConstants.NO_FORCE_TIME;
    private ActionType actionType = ActionType.FORCED;
    private String maintenanceSchedule;
    private String maintenanceWindowDuration;
    private String maintenanceWindowTimeZone;
    private boolean confirmationRequired;

    /**
     * Create a builder for a target distribution set assignment with the
     * mandatory fields
     * 
     * @param controllerId
     *            ID of the target
     * @param distributionSetId
     *            ID of the distribution set
     */
    public DeploymentRequestBuilder(final String controllerId, final Long distributionSetId) {
        this.controllerId = controllerId;
        this.distributionSetId = distributionSetId;
    }

    /**
     * Set an other {@link ActionType} than {@link ActionType#FORCED}
     * 
     * @param actionType
     *            type to used
     * @return builder
     */
    public DeploymentRequestBuilder setActionType(final ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    /**
     * Set a forceTime other than the default one.
     * 
     * @param forceTime
     *            at what time the type soft turns into forced.
     * @return builder
     */
    public DeploymentRequestBuilder setForceTime(final long forceTime) {
        this.forceTime = forceTime;
        return this;
    }

    /**
     * Set the weight of the action.
     * 
     * @param weight
     *            the priority given to the action.
     * @return builder
     */
    public DeploymentRequestBuilder setWeight(final Integer weight) {
        this.weight = weight;
        return this;
    }

    /**
     * Set a maintenanceWindow
     * 
     * @param maintenanceSchedule
     *            is the cron expression to be used for scheduling maintenance
     *            windows. Expression has 6 mandatory fields and 1 last optional
     *            field: "second minute hour dayofmonth month weekday year"
     * @param maintenanceWindowDuration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes
     * @param maintenanceWindowTimeZone
     *            is the time zone specified as +/-hh:mm offset from UTC, for
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     * @return builder
     */
    public DeploymentRequestBuilder setMaintenance(final String maintenanceSchedule,
            final String maintenanceWindowDuration, final String maintenanceWindowTimeZone) {
        this.maintenanceSchedule = maintenanceSchedule;
        this.maintenanceWindowDuration = maintenanceWindowDuration;
        this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
        return this;
    }

    /**
     * Set if a confirmation is required.
     * 
     * @param confirmationRequired
     *            if a confirmation is required for the {@link Action}
     * @return builder
     */
    public DeploymentRequestBuilder setConfirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return this;
    }

    /**
     * build the request
     * 
     * @return the request object
     */
    public DeploymentRequest build() {
        return new DeploymentRequest(controllerId, distributionSetId, actionType, forceTime, weight,
                maintenanceSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone, confirmationRequired);
    }

}
