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

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfigData {

    @NotEmpty
    private final Map<String, String> data;

    @Schema(example = "merge")
    private final DdiUpdateMode mode;

    /**
     * Constructor.
     *
     * @param data
     *            contains the attributes.
     * @param mode
     *            defines the mode of the update (replace, merge, remove)
     */
    @JsonCreator
    public DdiConfigData(@JsonProperty(value = "data") final Map<String, String> data,
            @JsonProperty(value = "mode") final DdiUpdateMode mode) {
        this.data = data;
        this.mode = mode;
    }

    public Map<String, String> getData() {
        return data;
    }

    public DdiUpdateMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "ConfigData [data=" + data + ", mode=" + mode + ", toString()=" + super.toString() + "]";
    }

}
