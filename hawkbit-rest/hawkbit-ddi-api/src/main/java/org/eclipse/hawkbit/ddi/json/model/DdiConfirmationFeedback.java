/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * New update actions require confirmation when confirmation flow is switched on. This is the feedback channel for
 * confirmation messages for DDI API. The confirmation message has a mandatory field confirmation with possible values:
 * "confirmed" and "denied".
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfirmationFeedback {

    public enum Confirmation {
        /**
         * Confirm the action.
         */
        CONFIRMED("confirmed"),

        /**
         * Deny the action.
         */
        DENIED("denied");


        private final String name;

        Confirmation(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    @NotNull
    @Valid
    @Schema(description = "Action confirmation state")
    private final Confirmation confirmation;

    @Schema(description = "(Optional) Individual status code", example = "200")
    private final Integer code;

    @Schema(description = "List of detailed message information", example = "[ \"Feedback message\" ]")
    private final List<String> details;

    /**
     * Constructs an confirmation-feedback
     *
     * @param confirmation  confirmation value for the action. Valid values are "Confirmed" and "Denied
     * @param code  code for confirmation
     * @param details messages
     */
    @JsonCreator public DdiConfirmationFeedback(
            @JsonProperty(value = "confirmation", required = true) final Confirmation confirmation,
            @JsonProperty(value = "code") final Integer code,
            @JsonProperty(value = "details") final List<String> details) {
        this.confirmation = confirmation;
        this.code = code;
        this.details = details;
    }

    public List<String> getDetails() {
        if (details == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(details);
    }
}