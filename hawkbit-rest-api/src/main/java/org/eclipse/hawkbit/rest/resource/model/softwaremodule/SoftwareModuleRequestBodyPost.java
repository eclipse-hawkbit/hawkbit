/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremodule;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request Body for SoftwareModule POST.
 *
 *
 *
 *
 */
public class SoftwareModuleRequestBodyPost {

    @ApiModelProperty(value = ApiModelProperties.NAME, required = true)
    @JsonProperty(required = true)
    private String name;

    @ApiModelProperty(value = ApiModelProperties.VERSION, required = true)
    @JsonProperty(required = true)
    private String version;

    @ApiModelProperty(value = ApiModelProperties.SOFTWARE_MODULE_TYPE, required = true, allowableValues = SoftwareModuleRest.SM_RUNTIME
            + "," + SoftwareModuleRest.SM_APPLICATION + "," + SoftwareModuleRest.SM_OS)
    @JsonProperty(required = true)
    private String type;

    @ApiModelProperty(value = ApiModelProperties.DESCRPTION)
    @JsonProperty
    private String description;

    @ApiModelProperty(value = ApiModelProperties.VENDOR)
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
     */
    public void setName(final String name) {
        this.name = name;
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
     */
    public void setVersion(final String version) {
        this.version = version;
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
     */
    public void setType(final String type) {
        this.type = type;
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
     */
    public void setDescription(final String description) {
        this.description = description;
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
     */
    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

}
