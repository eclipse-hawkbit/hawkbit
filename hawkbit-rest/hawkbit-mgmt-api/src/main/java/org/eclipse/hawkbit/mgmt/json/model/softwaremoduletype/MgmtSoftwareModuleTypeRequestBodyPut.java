/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
