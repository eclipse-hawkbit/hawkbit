/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.json.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Additional metadata to be provided for the target/device.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiMetadata {
    @JsonProperty
    @NotNull
    private final String key;

    @JsonProperty
    @NotNull
    private final String value;

    @JsonCreator
    public DdiMetadata(@JsonProperty("key") final String key, @JsonProperty("value")final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
