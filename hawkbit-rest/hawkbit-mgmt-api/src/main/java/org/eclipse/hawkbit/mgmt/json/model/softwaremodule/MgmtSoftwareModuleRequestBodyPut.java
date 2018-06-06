/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.softwaremodule;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for SoftwareModule PUT.
 *
 */
public class MgmtSoftwareModuleRequestBodyPut {

    @JsonProperty
    private String description;

    @JsonProperty
    private String vendor;

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
    public MgmtSoftwareModuleRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor
     *            the vendor to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPut setVendor(final String vendor) {
        this.vendor = vendor;
        return this;
    }

}
