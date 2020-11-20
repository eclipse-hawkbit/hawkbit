/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import java.time.Duration;

/**
 * Proxy for the Authentication view of system config window
 */
public class ProxySystemConfigPolling extends ProxySystemConfigWindow {
    private static final long serialVersionUID = 1L;

    private boolean pollingOverdue;
    private transient Duration pollingOverdueDuration;
    private boolean pollingTime;
    private transient Duration pollingTimeDuration;

    /**
     * Flag that indicates if the polling time option is enabled.
     *
     * @return <code>true</code> if the polling time is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isPollingTime() {
        return pollingTime;
    }

    /**
     * Sets the flag that indicates if the polling time option is enabled.
     *
     * @param pollingTime
     *            <code>true</code> if the polling time is enabled, otherwise
     *            <code>false</code>
     */
    public void setPollingTime(final boolean pollingTime) {
        this.pollingTime = pollingTime;
    }

    /**
     * Gets the pollingTimeDuration
     *
     * @return pollingTimeDuration
     */
    public Duration getPollingTimeDuration() {
        return pollingTimeDuration;
    }

    /**
     * Sets the pollingTimeDuration
     *
     * @param pollingTimeDuration
     *            System config window pollingTimeDuration
     */
    public void setPollingTimeDuration(final Duration pollingTimeDuration) {
        this.pollingTimeDuration = pollingTimeDuration;
    }

    /**
     * Flag that indicates if the polling overdue time option is enabled.
     *
     * @return <code>true</code> if the polling overdue time is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isPollingOverdue() {
        return pollingOverdue;
    }

    /**
     * Sets the flag that indicates if the polling overdue time option is enabled.
     *
     * @param pollingOverdue
     *            <code>true</code> if the polling overdue time is enabled,
     *            otherwise <code>false</code>
     */
    public void setPollingOverdue(final boolean pollingOverdue) {
        this.pollingOverdue = pollingOverdue;
    }

    /**
     * Gets the pollingOverdueDuration
     *
     * @return pollingOverdueDuration
     */
    public Duration getPollingOverdueDuration() {
        return pollingOverdueDuration;
    }

    /**
     * Sets the pollingOverdueDuration
     *
     * @param pollingOverdueDuration
     *            System config window pollingOverdueDuration
     */
    public void setPollingOverdueDuration(final Duration pollingOverdueDuration) {
        this.pollingOverdueDuration = pollingOverdueDuration;
    }

}
