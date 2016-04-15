/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.model;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Result information of the action progress which can by an intermediate or
 * final update.
 */
public class Result {

    @NotEmpty
    private final FinalResult finished;

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
     */
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

        @JsonValue
        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "Result [finished=" + finished + ", progress=" + progress + "]";
    }

}
