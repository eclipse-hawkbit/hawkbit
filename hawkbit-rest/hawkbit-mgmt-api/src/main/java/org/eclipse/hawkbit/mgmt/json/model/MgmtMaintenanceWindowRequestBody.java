/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(example = "10 12 14 3 8 ? 2023")
    private String schedule;

    @JsonProperty
    @Schema(example = "00:10:00")
    private String duration;

    @JsonProperty
    @Schema(example = "+00:00")
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
