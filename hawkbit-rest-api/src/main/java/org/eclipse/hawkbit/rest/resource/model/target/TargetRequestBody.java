/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request body for target PUT/POST commands.
 *
 *
 *
 *
 */
public class TargetRequestBody {
    @ApiModelProperty(value = ApiModelProperties.NAME, required = true)
    @JsonProperty(required = true)
    private String name;

    @ApiModelProperty(value = ApiModelProperties.DESCRPTION)
    private String description;

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID, required = true)
    @JsonProperty(required = true)
    private String controllerId;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the controllerId
     */
    public String getControllerId() {
        return controllerId;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @param controllerId
     *            the controllerId to set
     */
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

}
