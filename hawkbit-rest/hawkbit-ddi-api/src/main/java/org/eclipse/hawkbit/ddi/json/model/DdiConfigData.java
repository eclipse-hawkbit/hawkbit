/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfigData {

    @NotEmpty
    private final Map<String, String> data;

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
