/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for PUT/POST.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTagRequestBodyPut {

    @JsonProperty
    private String colour;

    @JsonProperty
    private String name;

    @JsonProperty
    private String description;

    public String getName() {
        return name;
    }

    public MgmtTagRequestBodyPut setName(final String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MgmtTagRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

    public MgmtTagRequestBodyPut setColour(final String colour) {
        this.colour = colour;
        return this;
    }

    public String getColour() {
        return colour;
    }
}
