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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of the Attribute THING_CREATED message.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmfCreateThing {

    @JsonProperty
    private String name;

    @JsonProperty
    private DmfAttributeUpdate attributeUpdate;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public DmfAttributeUpdate getAttributeUpdate() {
        return attributeUpdate;
    }

    public void setAttributeUpdate(final DmfAttributeUpdate attributeUpdate) {
        this.attributeUpdate = attributeUpdate;
    }
}
