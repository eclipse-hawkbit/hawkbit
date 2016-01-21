/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Result information of the action progress which can by an intermediate or
 * final update.
 *
 *
 *
 *
 *
 */
@ApiModel(ApiModelProperties.TARGET_RESULT_VALUE)
public class Result {

    @ApiModelProperty(value = ApiModelProperties.TARGET_RESULT_FINISHED, required = true)
    @NotEmpty
    private final FinalResult finished;

    @ApiModelProperty(value = ApiModelProperties.TARGET_RESULT_PROGRESS)
    private final Progress progress;

    /**
     * Constructor.
     *
     * @param finished
     *            as final result
     * @param progress
     *            if not yet finished
     */
    @JsonCreator
    public Result(@JsonProperty("finished") final FinalResult finished,
            @JsonProperty("progress") final Progress progress) {
        super();
        this.finished = finished;
        this.progress = progress;
    }

    public FinalResult getFinished() {
        return finished;
    }

    public Progress getProgress() {
        return progress;
    }

    /**
     * Defined status of the final result.
     *
     *
     *
     *
     *
     */
    @ApiModel(value = ApiModelProperties.TARGET_RESULT_FINISHED)
    public enum FinalResult {
        /**
         * Execution was successful.
         */
        SUCESS("success"),

        /**
         * Execution terminated with errors or without the expected result.
         */
        FAILURE("failure"),

        /**
         * No final result could be determined (yet).
         */
        NONE("none");

        private String name;

        private FinalResult(final String name) {
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
        return "Result [finished=" + finished + ", progress=" + progress + "]";
    }

}
