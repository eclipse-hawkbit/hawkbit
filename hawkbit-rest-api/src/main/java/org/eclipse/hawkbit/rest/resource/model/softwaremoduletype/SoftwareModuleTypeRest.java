/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremoduletype;

import org.eclipse.hawkbit.rest.resource.model.NamedEntityRest;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Software Module")
public class SoftwareModuleTypeRest extends NamedEntityRest {

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID, required = true)
    @JsonProperty(value = "id", required = true)
    private Long moduleId;

    @ApiModelProperty(value = ApiModelProperties.SM_TYPE_KEY)
    @JsonProperty
    private String key;

    @ApiModelProperty(value = ApiModelProperties.SM_MAX_ASSIGNMENTS)
    @JsonProperty
    private int maxAssignments;

    /**
     * @return the moduleId
     */
    public Long getModuleId() {
        return moduleId;
    }

    /**
     * @param moduleId
     *            the moduleId to set
     */
    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
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
