/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Details status information concerning the action processing.
 */
public class DdiStatus {

    @NotNull
    @Valid
    private final ExecutionStatus execution;

    @NotNull
    @Valid
    private final DdiResult result;

    private final List<String> details;

    /**
     * Constructor.
     *
     * @param execution
     *            status
     * @param result
     *            information
     * @param details
     *            as optional addition
     */
    @JsonCreator
    public DdiStatus(@JsonProperty("execution") final ExecutionStatus execution,
            @JsonProperty("result") final DdiResult result, @JsonProperty("details") final List<String> details) {
        super();
        this.execution = execution;
        this.result = result;
        this.details = details;
    }

    public ExecutionStatus getExecution() {
        return execution;
    }

    public DdiResult getResult() {
        return result;
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
     *
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
        RESUMED("resumed");

        private String name;

        ExecutionStatus(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "Status [execution=" + execution + ", result=" + result + ", details=" + details + "]";
    }

}
