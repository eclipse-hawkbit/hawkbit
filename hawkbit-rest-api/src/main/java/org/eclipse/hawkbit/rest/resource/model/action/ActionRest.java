/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.action;

import org.eclipse.hawkbit.rest.resource.model.BaseEntityRest;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A json annotated rest model for Action to RESTful API representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("SP Action")
public class ActionRest extends BaseEntityRest {

    /**
     * API definition for {@link UpdateAction}.
     */
    public static final String ACTION_UPDATE = "update";

    /**
     * API definition for {@link CancelAction}.
     */
    public static final String ACTION_CANCEL = "cancel";

    public static final String ACTION_FINISHED = "finished";

    public static final String ACTION_PENDING = "pending";

    @ApiModelProperty(value = ApiModelProperties.ITEM_ID)
    @JsonProperty("id")
    private Long actionId;

    @ApiModelProperty(value = ApiModelProperties.ACTION_TYPE, allowableValues = ACTION_UPDATE + "," + ACTION_CANCEL)
    @JsonProperty
    private String type;

    @ApiModelProperty(value = ApiModelProperties.ACTION_EXECUTION_STATUS, allowableValues = ACTION_FINISHED + ","
            + ACTION_PENDING)
    @JsonProperty
    private String status;

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return the actionId
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     * @param actionId
     *            the actionId to set
     */
    public void setActionId(final Long actionId) {
        this.actionId = actionId;
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

}
