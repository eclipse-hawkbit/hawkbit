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

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of the Attribute Update message.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfAttributeUpdate {

    private final Map<String, String> attributes;
    private final DmfUpdateMode mode;

    @JsonCreator
    public DmfAttributeUpdate(
            @JsonProperty("attributes") final Map<String, String> attributes,
            @JsonProperty("mode") final DmfUpdateMode mode) {
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
        this.mode = mode;
    }
}