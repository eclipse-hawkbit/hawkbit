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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The action that has to be stopped by the target.
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiCancelActionToStop {

    @NotNull
    @Schema(description = "Id of the action that needs to be canceled (typically identical to id field on the cancel action itself)", example = "11")
    private final String stopId;

    /**
     * Parameterized constructor.
     *
     * @param stopId ID of the action to be stopped
     */
    @JsonCreator
    public DdiCancelActionToStop(@JsonProperty("stopId") final String stopId) {
        this.stopId = stopId;
    }
}