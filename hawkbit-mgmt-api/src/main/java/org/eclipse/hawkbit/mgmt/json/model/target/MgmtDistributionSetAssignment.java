/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.MgmtMaintenanceWindow;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 *
 */
public class MgmtDistributionSetAssignment extends MgmtId {
    private long forcetime;
    private MgmtActionType type;

    /**
     * {@link MgmtMaintenanceWindow} object defining a schedule, duration and
     * timezone.
     */
    private MgmtMaintenanceWindow maintenanceWindow;

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
     * Returns {@link MgmtMaintenanceWindow} for distribution set assignment.
     *
     * @return {@link MgmtMaintenanceWindow}.
     */
    public MgmtMaintenanceWindow getMaintenanceWindow() {
        return maintenanceWindow;
    }

    /**
     * Sets {@link MgmtMaintenanceWindow} for distribution set assignment.
     *
     * @param maintenanceWindow
     *            as {@link MgmtMaintenanceWindow}.
     */
    public void setMaintenanceWindow(final MgmtMaintenanceWindow maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }
}
