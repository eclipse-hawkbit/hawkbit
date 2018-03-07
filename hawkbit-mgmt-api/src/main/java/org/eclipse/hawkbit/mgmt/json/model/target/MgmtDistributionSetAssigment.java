/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import org.eclipse.hawkbit.mgmt.json.model.MaintenanceWindow;
import org.eclipse.hawkbit.mgmt.json.model.MgmtId;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtActionType;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 *
 */
public class MgmtDistributionSetAssigment extends MgmtId {
    private long forcetime;
    private MgmtActionType type;

    /**
     * {@link MaintenanceWindow} object defining a schedule, duration and
     * timezone.
     */
    private MaintenanceWindow maintenanceWindow = null;

    /**
     * @return the type
     */
    public MgmtActionType getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final MgmtActionType type) {
        this.type = type;
    }

    /**
     * @return the forcetime
     */
    public long getForcetime() {
        return forcetime;
    }

    /**
     * @param forcetime
     *            the forcetime to set
     */
    public void setForcetime(final long forcetime) {
        this.forcetime = forcetime;
    }

    /**
     * Returns {@link MaintenanceWindow} for distribution set assignment.
     *
     * @return {@link MaintenanceWindow}.
     */
    public MaintenanceWindow getMaintenanceWindow() {
        return maintenanceWindow;
    }

    /**
     * Sets {@link MaintenanceWindow} for distribution set assignment.
     *
     * @param maintenanceWindow
     *            as {@link MaintenanceWindow}.
     */
    public void setMaintenanceWindow(MaintenanceWindow maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }
}
