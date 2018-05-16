/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of Target for assignment operations (ID only).
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentRequestBody {

    @JsonProperty
    private String id;

    private long forcetime;

    private MgmtActionType type;

    /**
     * {@link MgmtMaintenanceWindowRequestBody} object containing schedule,
     * duration and timezone.
     */
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public MgmtActionType getType() {
        return type;
    }

    public void setType(final MgmtActionType type) {
        this.type = type;
    }

    public long getForcetime() {
        return forcetime;
    }

    public void setForcetime(final long forcetime) {
        this.forcetime = forcetime;
    }

    public MgmtMaintenanceWindowRequestBody getMaintenanceWindow() {
        return maintenanceWindow;
    }

    public void setMaintenanceWindow(final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

}
