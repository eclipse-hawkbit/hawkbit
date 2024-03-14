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
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Request body for maintenance window PUT/POST commands, based on a schedule
 * defined as cron expression, duration in HH:mm:ss format and time zone as
 * offset from UTC.
 */
@Data
@Accessors(chain = true)
@ToString
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtMaintenanceWindowRequestBody {

    @JsonProperty
    @Schema(description = """
            Schedule for the maintenance window start in quartz cron notation, such as '0 15 10 * * ? 2018'
            for 10:15am every day during the year 2018""", example = "10 12 14 3 8 ? 2023")
    private String schedule;

    @JsonProperty
    @Schema(description = "Duration of the window, such as '02:00:00' for 2 hours", example = "00:10:00")
    private String duration;

    @JsonProperty
    @Schema(description = "A time-zone offset from Greenwich/UTC, such as '+02:00'", example = "+00:00")
    private String timezone;
}