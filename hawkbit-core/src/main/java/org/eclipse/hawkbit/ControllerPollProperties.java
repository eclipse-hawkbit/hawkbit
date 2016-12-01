/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;

/**
 * Defines global configuration for the controllers/clients on the provisioning
 * targets/devices.
 * 
 * 
 * Note: many of the controller related properties can be overridden on tenant
 * level. As a result they are not defined here but in
 * {@link TenantConfigurationKey} and injected using {@link EnvironmentAware}.
 * 
 */
@ConfigurationProperties(prefix = "hawkbit.controller")
public class ControllerPollProperties {

    /**
     * Maximum polling time that can be configured by a tenant in HH:MM:SS
     * notation.
     */
    private String maxPollingTime = "23:59:59";

    /**
     * Minimum polling time that can be configured by a tenant in HH:MM:SS
     * notation.
     */
    private String minPollingTime = "00:00:30";

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

}
