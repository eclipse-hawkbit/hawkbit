/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremoduletype;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for SoftwareModuleType PUT.
 *
 */
public class SoftwareModuleTypeRequestBodyPut {

    @JsonProperty
    private String description;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            the description to set
     *
     * @return updated body
     */
    public SoftwareModuleTypeRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

}
