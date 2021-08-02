/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
