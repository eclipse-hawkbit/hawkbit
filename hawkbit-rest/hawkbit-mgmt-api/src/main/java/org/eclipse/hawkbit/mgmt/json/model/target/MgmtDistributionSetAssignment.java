/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 *
 */
public class MgmtDistributionSetAssignment extends MgmtId {

    private long forcetime;
    @JsonProperty(required = false)
    private Integer weight;
    @JsonProperty(required = false)
    private Boolean confirmationRequired;
    private MgmtActionType type;
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

    /**
     * Constructor
     * 
     * @param id
     *            ID of object
     */
    @JsonCreator
    public MgmtDistributionSetAssignment(@JsonProperty(required = true, value = "id") final Long id) {
        super(id);
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

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final int weight) {
        this.weight = weight;
    }

    /**
     * Returns {@link MgmtMaintenanceWindowRequestBody} for distribution set
     * assignment.
     *
     * @return {@link MgmtMaintenanceWindowRequestBody}.
     */
    public MgmtMaintenanceWindowRequestBody getMaintenanceWindow() {
        return maintenanceWindow;
    }

    /**
     * Sets {@link MgmtMaintenanceWindowRequestBody} for distribution set
     * assignment.
     *
     * @param maintenanceWindow
     *            as {@link MgmtMaintenanceWindowRequestBody}.
     */
    public void setMaintenanceWindow(final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
