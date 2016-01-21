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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * A generic abstract rest model which contains only a ID for use-case e.g.
 * which allows only posting or putting an ID into the request body, e.g. for
 * assignments.
 * 
 *
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdRest {

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID)
    @JsonProperty
    private Long id;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

}
