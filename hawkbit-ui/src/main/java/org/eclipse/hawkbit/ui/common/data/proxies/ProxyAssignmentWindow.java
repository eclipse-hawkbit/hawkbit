/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.ui.common.data.aware.ActionTypeAware;

/**
 * Proxy entity representing assignment popup window bean.
 */
public class ProxyAssignmentWindow implements Serializable, ActionTypeAware {

    private static final long serialVersionUID = 1L;

    private ActionType actionType;
    private Long forcedTime;
    private boolean isMaintenanceWindowEnabled;
    private String maintenanceSchedule;
    private String maintenanceDuration;
    private String maintenanceTimeZone;

    /**
     * Gets the actionType
     *
     * @return actionType
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Sets the actionType
     *
     * @param actionType
     *          Action type
     */
    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }

    /**
     * Gets the forcedTime
     *
     * @return forcedTime
     */
    public Long getForcedTime() {
        return forcedTime;
    }

    /**
     * Sets the forcedTime
     *
     * @param forcedTime
     *          Forced time
     */
    public void setForcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
    }

    /**
     * Flag that indicates if maintenance window is enabled.
     *
     * @return <code>true</code> if the window is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isMaintenanceWindowEnabled() {
        return isMaintenanceWindowEnabled;
    }

    /**
     * Sets the flag that indicates if maintenance window is enabled
     *
     * @param isMaintenanceWindowEnabled
     *            <code>true</code> if the window is enabled, otherwise
     *            <code>false</code>
     */
    public void setMaintenanceWindowEnabled(boolean isMaintenanceWindowEnabled) {
        this.isMaintenanceWindowEnabled = isMaintenanceWindowEnabled;
    }

    /**
     * Gets the maintenanceSchedule
     *
     * @return maintenanceSchedule
     */
    public String getMaintenanceSchedule() {
        return maintenanceSchedule;
    }

    /**
     * Sets the maintenanceSchedule
     *
     * @param maintenanceSchedule
     *          Maintenance schedule
     */
    public void setMaintenanceSchedule(String maintenanceSchedule) {
        this.maintenanceSchedule = maintenanceSchedule;
    }

    /**
     * Gets the duration of maintenance
     *
     * @return maintenanceDuration
     */
    public String getMaintenanceDuration() {
        return maintenanceDuration;
    }

    /**
     * Sets the maintenanceDuration
     *
     * @param maintenanceDuration
     *          Duration of maintenance
     */
    public void setMaintenanceDuration(String maintenanceDuration) {
        this.maintenanceDuration = maintenanceDuration;
    }

    /**
     * Gets the maintenanceTimeZone
     *
     * @return Time zone of Maintenance
     */
    public String getMaintenanceTimeZone() {
        return maintenanceTimeZone;
    }

    /**
     * Sets the maintenanceTimeZone
     *
     * @param maintenanceTimeZone
     *          Time zone of Maintenance
     */
    public void setMaintenanceTimeZone(String maintenanceTimeZone) {
        this.maintenanceTimeZone = maintenanceTimeZone;
    }
}
