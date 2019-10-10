/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of the Attribute Update message.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfAttributeUpdate {

    @JsonProperty
    private final Map<String, String> attributes = new HashMap<>();

    @JsonProperty
    private DmfUpdateMode mode;

    public DmfUpdateMode getMode() {
        return mode;
    }

    public void setMode(final DmfUpdateMode mode) {
        this.mode = mode;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
