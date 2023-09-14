/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
