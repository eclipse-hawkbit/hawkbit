/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.action;

import org.eclipse.hawkbit.mgmt.json.model.MgmtBaseEntity;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A json annotated rest model for Action to RESTful API representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtAction extends MgmtBaseEntity {

    /**
     * API definition for action in update mode.
     */
    public static final String ACTION_UPDATE = "update";

    /**
     * API definition for action in canceling.
     */
    public static final String ACTION_CANCEL = "cancel";

    /**
     * API definition for action completed.
     */
    public static final String ACTION_FINISHED = "finished";

    /**
     * API definition for action still active.
     */
    public static final String ACTION_PENDING = "pending";

    @JsonProperty("id")
    private Long actionId;

    @JsonProperty
    private String type;

    @JsonProperty
    private String status;

    @JsonProperty
    private Long forceTime;

    @JsonProperty
    private MgmtActionType forceType;

    public Long getForceTime() {
        return forceTime;
    }

    public void setForceTime(final Long forceTime) {
        this.forceTime = forceTime;
    }

    public MgmtActionType getForceType() {
        return forceType;
    }

    public void setForceType(final MgmtActionType forceType) {
        this.forceType = forceType;
    }

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
