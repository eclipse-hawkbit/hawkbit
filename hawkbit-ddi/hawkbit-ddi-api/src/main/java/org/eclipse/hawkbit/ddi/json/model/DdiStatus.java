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

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Details status information concerning the action processing.
 */
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Target action status")
public class DdiStatus {

    @NotNull
    @Valid
    @Schema(description = "Status of the action execution")
    private final ExecutionStatus execution;
    @NotNull
    @Valid
    @Schema(description = "Result of the action execution")
    private final DdiResult result;
    @Schema(description = "(Optional) Individual status code", example = "200")
    private final Integer code;
    @Schema(description = "List of details message information", example = "[ \"Some feedback\" ]")
    private final List<String> details;

    /**
     * Constructor.
     *
     * @param execution status
     * @param result information
     * @param code as optional code (can be null)
     * @param details as optional addition
     */
    @JsonCreator
    public DdiStatus(@JsonProperty("execution") final ExecutionStatus execution,
            @JsonProperty("result") final DdiResult result, @JsonProperty("code") final Integer code,
            @JsonProperty("details") final List<String> details) {
        this.execution = execution;
        this.result = result;
        this.code = code;
        this.details = details;
    }

    public List<String> getDetails() {
        if (details == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(details);
    }

    /**
     * The element status contains information about the execution of the
     * operation.
     */
    public enum ExecutionStatus {
        /**
         * Execution of the action has finished.
         */
        CLOSED("closed"),

        /**
         * Execution has started but has not yet finished.
         */
        PROCEEDING("proceeding"),

        /**
         * Execution was suspended from outside.
         */
        CANCELED("canceled"),

        /**
         * Action has been noticed and is intended to run.
         */
        SCHEDULED("scheduled"),

        /**
         * Action was not accepted.
         */
        REJECTED("rejected"),

        /**
         * Action is started after a reset, power loss, etc.
         */
        RESUMED("resumed"),

        /**
         * The action has been downloaded by the target.
         */
        DOWNLOADED("downloaded"),

        /**
         * Target starts to download.
         */
        DOWNLOAD("download");

        private String name;

        ExecutionStatus(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }
}