/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindowRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 *
 */
public class MgmtDistributionSetAssignment extends MgmtId {
    private long forcetime;
    private MgmtActionType type;

    /**
     * {@link MgmtMaintenanceWindowRequestBody} object defining a schedule,
     * duration and timezone.
     */
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;

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
}
