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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Action to RESTful API representation.
 *
 *
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("id")
    private Long actionId;

    @JsonProperty
    private String type;

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
