/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for maintenance window PUT/POST commands, based on a schedule
 * defined as cron expression, duration in HH:mm:ss format and time zone as
 * offset from UTC.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtMaintenanceWindowRequestBody {
    @JsonProperty
    private String schedule;

    @JsonProperty
    private String duration;

    @JsonProperty
    private String timezone;

    public String getSchedule() {
        return schedule;
    }

    /**
     * @param schedule
     *            is the cron expression to be used for scheduling maintenance
     *            window(s). Expression has 6 mandatory fields and a last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     */
    public void setSchedule(final String schedule) {
        this.schedule = schedule;
    }

    public String getDuration() {
        return duration;
    }

    /**
     * @param duration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes.
     */
    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getTimezone() {
        return timezone;
    }

    /**
     * @param timezone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     */
    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }
}
