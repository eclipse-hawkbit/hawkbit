/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defines global configuration for the controllers/clients on the provisioning
 * targets/devices.
 * 
 * 
 * Note: many of the controller related properties can be overridden on tenant
 * level.
 * 
 */
@ConfigurationProperties(prefix = "hawkbit.controller")
public class ControllerPollProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Maximum polling time that can be configured system wide and by tenant in
     * HH:MM:SS notation.
     */
    private String maxPollingTime = "23:59:59";

    /**
     * Minimum polling time that can be configured by a tenant in HH:MM:SS
     * notation.
     */
    private String minPollingTime = "00:00:30";

    /**
     * Controller polling time that can be configured system wide and by tenant
     * in HH:MM:SS notation.
     */
    private String pollingTime = "00:05:00";

    /**
     * Controller polling overdue time that can be configured system wide and by
     * tenant in HH:MM:SS notation.
     */
    private String pollingOverdueTime = "00:05:00";

    /**
     * This configuration value is used to change the polling interval so that
     * controller tries to poll at least these many times between the last
     * polling and before start of maintenance window. The polling interval is
     * bounded by configured pollingTime and minPollingTime. The polling
     * interval is modified as per following scheme: pollingTime(@time=t) =
     * (maintenanceWindowStartTime - t)/maintenanceWindowPollCount.
     */
    private int maintenanceWindowPollCount = 3;

    public String getPollingTime() {
        return pollingTime;
    }

    public void setPollingTime(final String pollingTime) {
        this.pollingTime = pollingTime;
    }

    public String getPollingOverdueTime() {
        return pollingOverdueTime;
    }

    public void setPollingOverdueTime(final String pollingOverdueTime) {
        this.pollingOverdueTime = pollingOverdueTime;
    }

    public String getMaxPollingTime() {
        return maxPollingTime;
    }

    public void setMaxPollingTime(final String maxPollingTime) {
        this.maxPollingTime = maxPollingTime;
    }

    public String getMinPollingTime() {
        return minPollingTime;
    }

    public void setMinPollingTime(final String minPollingTime) {
        this.minPollingTime = minPollingTime;
    }

    /**
     * Returns poll count for maintenance window
     * ({@link ControllerPollProperties#maintenanceWindowPollCount}).
     *
     * @return maintenanceWindowPollCount as int.
     */
    public int getMaintenanceWindowPollCount() {
        return maintenanceWindowPollCount;
    }

    /**
     * Sets poll count for maintenance window
     * ({@link ControllerPollProperties#maintenanceWindowPollCount}).
     *
     * @param maintenanceWindowPollCount.
     */
    public void setMaintenanceWindowPollCount(int maintenanceWindowPollCount) {
        this.maintenanceWindowPollCount = maintenanceWindowPollCount;
    }
}
