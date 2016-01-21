/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import java.util.Map;

import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * feedback channel for ConfigData action.
 *
 *
 *
 *
 *
 */
@ApiModel("Configuration or metadata that is reponded by the target")
public class ConfigData extends ActionFeedback {

    @ApiModelProperty(ApiModelProperties.TARGET_CONFIG_DATA)
    @NotEmpty
    private final Map<String, String> data;

    /**
     * Constructor.
     *
     * @param id
     *            of the actions the feedback is for
     * @param time
     *            of the feedback
     * @param status
     *            is the feedback itself
     * @param data
     *            contains the attributes.
     *
     */
    @JsonCreator
    public ConfigData(@JsonProperty(value = "id") final Long id, @JsonProperty(value = "time") final String time,
            @JsonProperty(value = "status") final Status status,
            @JsonProperty(value = "data") final Map<String, String> data) {
        super(id, time, status);
        this.data = data;
    }

    /**
     * @return the data
     */
    public Map<String, String> getData() {
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ConfigData [data=" + data + ", toString()=" + super.toString() + "]";
    }

}
