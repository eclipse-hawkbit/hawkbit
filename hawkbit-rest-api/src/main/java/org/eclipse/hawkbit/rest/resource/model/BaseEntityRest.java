/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for BaseEntity to RESTful API representation.
 *
 *
 *
 *
 */
public abstract class BaseEntityRest extends ResourceSupport {

    @ApiModelProperty(value = ApiModelProperties.CREATED_BY)
    @JsonProperty
    private String createdBy;

    @ApiModelProperty(value = ApiModelProperties.CREATED_AT)
    @JsonProperty
    private Long createdAt;

    @ApiModelProperty(value = ApiModelProperties.LAST_MODIFIED_BY)
    @JsonProperty
    private String lastModifiedBy;

    @ApiModelProperty(value = ApiModelProperties.LAST_MODIFIED_AT)
    @JsonProperty
    private Long lastModifiedAt;

    /**
     * @return the createdBy
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy
     *            the createdBy to set
     */
    @JsonIgnore
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the createdAt
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt
     *            the createdAt to set
     */
    @JsonIgnore
    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy
     *            the lastModifiedBy to set
     */
    @JsonIgnore
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the lastModifiedAt
     */
    public Long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * @param lastModifiedAt
     *            the lastModifiedAt to set
     */
    @JsonIgnore
    public void setLastModifiedAt(final Long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
