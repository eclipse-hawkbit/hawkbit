/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.dmf.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JSON representation of the Attribute THING_CREATED message.
 */
@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfCreateThing {

    private final String name;
    private final String type;
    private final DmfAttributeUpdate attributeUpdate;

    @JsonCreator
    public DmfCreateThing(
            @JsonProperty("name") final String name,
            @JsonProperty("type") final String type,
            @JsonProperty("attributeUpdate") final DmfAttributeUpdate attributeUpdate) {
        this.name = name;
        this.type = type;
        this.attributeUpdate = attributeUpdate;
    }
}