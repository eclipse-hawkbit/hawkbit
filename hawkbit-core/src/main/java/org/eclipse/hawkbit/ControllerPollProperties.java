/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defines the polling time for the controllers in HH:MM:SS notation.
 * 
 */

@ConfigurationProperties(prefix = "hawkbit.controller")
public class ControllerPollProperties {

    private String pollingTime = "00:05:00";
    private String pollingOverdueTime = "00:05:00";
    private String maxPollingTime = "23:59:00";
    private String minPollingTime = "00:00:30";

    public String getPollingTime() {
        return pollingTime;
    }

    public void setPollingTime(final String pollingTime) {
        this.pollingTime = pollingTime;
    }

    public String getPollingOverdueTime() {
        return pollingOverdueTime;
    }

    public void setPollingOverdueTime(final String pollingOverdue) {
        this.pollingOverdueTime = pollingOverdue;
    }

    public String getMaxPollingTime() {
        return maxPollingTime;
    }

    public void setMaxPollingTime(String maxPollingTime) {
        this.maxPollingTime = maxPollingTime;
    }

    public String getMinPollingTime() {
        return minPollingTime;
    }

    public void setMinPollingTime(String minPollingTime) {
        this.minPollingTime = minPollingTime;
    }
}
