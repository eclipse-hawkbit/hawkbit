/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Details status information concerning the action processing.
 *
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.TARGET_STATUS)
public class Status {

    @ApiModelProperty(value = ApiModelProperties.TARGET_EXEC_STATUS, required = true)
    @NotNull
    private final ExecutionStatus execution;

    @ApiModelProperty(value = ApiModelProperties.TARGET_RESULT_VALUE, required = true)
    @NotNull
    private final Result result;

    @ApiModelProperty(value = ApiModelProperties.TARGET_RESULT_DETAILS)
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
    public Status(@JsonProperty("execution") final ExecutionStatus execution,
            @JsonProperty("result") final Result result, @JsonProperty("details") final List<String> details) {
        super();
        this.execution = execution;
        this.result = result;
        this.details = details;
    }

    public ExecutionStatus getExecution() {
        return execution;
    }

    public Result getResult() {
        return result;
    }

    public List<String> getDetails() {
        return details;
    }

    /**
     * The element status contains information about the execution of the
     * operation.
     *
     *
     *
     *
     *
     */
    @ApiModel(ApiModelProperties.TARGET_EXEC_STATUS)
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

        private ExecutionStatus(final String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        @JsonValue
        public String getName() {
            return name;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Status [execution=" + execution + ", result=" + result + ", details=" + details + "]";
    }

}
