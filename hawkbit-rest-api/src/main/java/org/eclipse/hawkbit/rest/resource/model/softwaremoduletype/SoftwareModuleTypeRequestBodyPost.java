/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremoduletype;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request Body for SoftwareModuleType POST.
 *
 *
 *
 *
 */
public class SoftwareModuleTypeRequestBodyPost {

    @ApiModelProperty(value = ApiModelProperties.NAME, required = true)
    @JsonProperty(required = true)
    private String name;

    @ApiModelProperty(value = ApiModelProperties.DESCRPTION)
    @JsonProperty
    private String description;

    @ApiModelProperty(value = ApiModelProperties.SM_TYPE_KEY)
    @JsonProperty
    private String key;

    @ApiModelProperty(value = ApiModelProperties.SM_MAX_ASSIGNMENTS)
    @JsonProperty
    private int maxAssignments;

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
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @return the maxAssignments
     */
    public int getMaxAssignments() {
        return maxAssignments;
    }

    /**
     * @param maxAssignments
     *            the maxAssignments to set
     */
    public void setMaxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

}
