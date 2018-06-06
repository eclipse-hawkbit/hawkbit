/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for SoftwareModuleType PUT.
 *
 */
public class MgmtSoftwareModuleTypeRequestBodyPut {

    @JsonProperty
    private String description;

    @JsonProperty
    private String colour;

    public String getDescription() {
        return description;
    }

    public MgmtSoftwareModuleTypeRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

    public String getColour() {
        return colour;
    }

    public MgmtSoftwareModuleTypeRequestBodyPut setColour(final String colour) {
        this.colour = colour;
        return this;
    }

}
