/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit;

import java.io.Serializable;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defines global configuration for the controllers/clients on the provisioning
 * targets/devices.
 * 
 * Note: many of the controller related properties can be overridden on tenant
 * level.
 */
@Data
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
}
