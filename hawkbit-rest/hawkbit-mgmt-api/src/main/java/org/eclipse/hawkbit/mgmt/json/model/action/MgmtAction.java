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
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindow;
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
    private String detailStatus;

    @JsonProperty
    private Long forceTime;

    @JsonProperty(value = "forceType")
    private MgmtActionType actionType;

    @JsonProperty
    private Integer weight;

    @JsonProperty
    private MgmtMaintenanceWindow maintenanceWindow;

    @JsonProperty
    private Long rollout;

    @JsonProperty
    private String rolloutName;

    @JsonProperty
    private Integer lastStatusCode;

    public MgmtMaintenanceWindow getMaintenanceWindow() {
        return maintenanceWindow;
    }

    public void setMaintenanceWindow(final MgmtMaintenanceWindow maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

    public Long getForceTime() {
        return forceTime;
    }

    public void setForceTime(final Long forceTime) {
        this.forceTime = forceTime;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public MgmtActionType getActionType() {
        return actionType;
    }

    public void setActionType(final MgmtActionType actionType) {
        this.actionType = actionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Long getRollout() {
        return rollout;
    }

    public void setRollout(final Long rollout) {
        this.rollout = rollout;
    }

    public String getRolloutName() {
        return rolloutName;
    }

    public void setRolloutName(final String rolloutName) {
        this.rolloutName = rolloutName;
    }

    public String getDetailStatus() {
        return detailStatus;
    }

    public void setDetailStatus(final String detailStatus) {
        this.detailStatus = detailStatus;
    }

    public Integer getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(final Integer lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

}
