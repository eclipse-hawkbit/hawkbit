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
 * Request Body for SoftwareModule POST.
 *
 */
public class MgmtSoftwareModuleRequestBodyPost {

    @JsonProperty(required = true)
    private String name;

    @JsonProperty(required = true)
    private String version;

    @JsonProperty(required = true)
    private String type;

    @JsonProperty
    private String description;

    @JsonProperty
    private String vendor;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setVersion(final String version) {
        this.version = version;
        return this;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleRequestBodyPost setType(final String type) {
        this.type = type;
        return this;
    }

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
    public MgmtSoftwareModuleRequestBodyPost setDescription(final String description) {
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
    public MgmtSoftwareModuleRequestBodyPost setVendor(final String vendor) {
        this.vendor = vendor;
        return this;
    }

}
