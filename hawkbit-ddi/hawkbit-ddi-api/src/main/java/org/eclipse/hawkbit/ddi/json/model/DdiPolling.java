/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Polling interval for the SP target.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Suggested sleep time between polls")
public class DdiPolling {

    @Schema(description = "Sleep time in HH:mm:ss notation", pattern = "HH:mm:ss", example = "12:00:00")
    private final String sleep;

    /**
     * Constructor.
     *
     * @param sleep between polls
     */
    @JsonCreator
    public DdiPolling(@JsonProperty("sleep") final String sleep) {
        this.sleep = sleep;
    }
}