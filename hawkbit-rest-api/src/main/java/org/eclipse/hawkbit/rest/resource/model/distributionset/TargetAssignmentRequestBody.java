/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionset;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Request Body of Target for assignment operations (ID only).
 * 
 *
 *
 *
 */
@ApiModel("Target ID")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetAssignmentRequestBody {

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID)
    @JsonProperty
    private String id;

    @ApiModelProperty(value = "The force time in case type is 'timeforced'", required = false)
    private long forcetime;

    @ApiModelProperty(value = "The type of action to assign, default 'forced'", required = false, allowableValues = "soft,forced,timeforced")
    private ActionTypeRest type;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return the type
     */
    public ActionTypeRest getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final ActionTypeRest type) {
        this.type = type;
    }

    /**
     * @return the forcetime
     */
    public long getForcetime() {
        return forcetime;
    }

    /**
     * @param forcetime
     *            the forcetime to set
     */
    public void setForcetime(final long forcetime) {
        this.forcetime = forcetime;
    }

}
