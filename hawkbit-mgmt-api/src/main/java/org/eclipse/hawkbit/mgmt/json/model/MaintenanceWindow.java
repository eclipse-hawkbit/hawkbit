/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * JSON model for Management API to define the maintenance window based on a
 * schedule defined as cron expression, duration in HH:mm:ss format and time
 * zone as offset from UTC.
 */
public class MaintenanceWindow {

    private String maintenanceSchedule;
    private String maintenanceWindowDuration;
    private String maintenanceWindowTimeZone;

    /**
     * Sets the maintenance schedule.
     *
     * @param maintenanceSchedule
     *            is the cron expression to be used for scheduling maintenance
     *            window(s). Expression has 6 mandatory fields and a last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     */
    @JsonSetter("schedule")
    public void setMaintenanceSchedule(String maintenanceSchedule) {
        this.maintenanceSchedule = maintenanceSchedule;
    }

    /**
     * Sets the maintenance window duration.
     *
     * @param maintenanceWindowDuration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes.
     */
    @JsonSetter("duration")
    public void setMaintenanceWindowDuration(String maintenanceWindowDuration) {
        this.maintenanceWindowDuration = maintenanceWindowDuration;
    }

    /**
     * Sets the maintenance window timezone.
     *
     * @param maintenanceWindowTimeZone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     */
    @JsonSetter("timezone")
    public void setMaintenanceWindowTimeZone(String maintenanceWindowTimeZone) {
        this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
    }

    /**
     * Returns the maintenance schedule for the {@link Action}.
     *
     * @return cron expression as {@link String}.
     */
    public String getMaintenanceSchedule() {
        return maintenanceSchedule;
    }

    /**
     * Returns the duration of maintenance window for the {@link Action}.
     *
     * @return duration in HH:mm:ss format as {@link String}.
     */
    public String getMaintenanceWindowDuration() {
        return maintenanceWindowDuration;
    }

    /**
     * Returns the timezone of maintenance window for the {@link Action}.
     *
     * @return the timezone offset from UTC in +/-hh:mm as {@link String}.
     */
    public String getMaintenanceWindowTimeZone() {
        return maintenanceWindowTimeZone;
    }
}
