/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCleanupConfigurationItem;

/**
 * Proxy for the Repository view of system config window
 */
public class ProxySystemConfigRepository extends ProxySystemConfigWindow {
    private static final long serialVersionUID = 1L;

    private boolean actionAutoclose;
    private boolean actionAutocleanup;
    private boolean multiAssignments;
    private ActionAutoCleanupConfigurationItem.ActionStatusOption actionCleanupStatus;
    private String actionExpiryDays;

    /**
     * Flag that indicates if the actionAutocleanup option is enabled.
     *
     * @return <code>true</code> if the actionAutocleanup is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isActionAutocleanup() {
        return actionAutocleanup;
    }

    /**
     * Sets the flag that indicates if the actionAutocleanup option is enabled.
     *
     * @param actionAutocleanup
     *            <code>true</code> if the actionAutocleanup is enabled, otherwise
     *            <code>false</code>
     */
    public void setActionAutocleanup(final boolean actionAutocleanup) {
        this.actionAutocleanup = actionAutocleanup;
    }

    /**
     * Flag that indicates if the multiAssignments option is enabled.
     *
     * @return <code>true</code> if the multiAssignments is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isMultiAssignments() {
        return multiAssignments;
    }

    /**
     * Sets the flag that indicates if the multiAssignments option is enabled.
     *
     * @param multiAssignments
     *            <code>true</code> if the multiAssignments is enabled, otherwise
     *            <code>false</code>
     */
    public void setMultiAssignments(final boolean multiAssignments) {
        this.multiAssignments = multiAssignments;
    }

    /**
     * Flag that indicates if the actionAutoclose option is enabled.
     *
     * @return <code>true</code> if the actionAutoclose is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isActionAutoclose() {
        return actionAutoclose;
    }

    /**
     * Sets the flag that indicates if the actionAutoclose option is enabled.
     *
     * @param actionAutoclose
     *            <code>true</code> if the actionAutoclose is enabled, otherwise
     *            <code>false</code>
     */
    public void setActionAutoclose(final boolean actionAutoclose) {
        this.actionAutoclose = actionAutoclose;
    }

    /**
     * Gets the actionExpiryDays
     *
     * @return actionExpiryDays
     */
    public String getActionExpiryDays() {
        return actionExpiryDays;
    }

    /**
     * Sets the actionExpiryDays
     *
     * @param actionExpiryDays
     *            System config window actionExpiryDays
     */
    public void setActionExpiryDays(final String actionExpiryDays) {
        this.actionExpiryDays = actionExpiryDays;
    }

    /**
     * Gets the actionCleanupStatus
     *
     * @return actionCleanupStatus
     */
    public ActionAutoCleanupConfigurationItem.ActionStatusOption getActionCleanupStatus() {
        return actionCleanupStatus;
    }

    /**
     * Sets the actionCleanupStatus
     *
     * @param actionCleanupStatus
     *            System config window actionCleanupStatus
     */
    public void setActionCleanupStatus(
            final ActionAutoCleanupConfigurationItem.ActionStatusOption actionCleanupStatus) {
        this.actionCleanupStatus = actionCleanupStatus;
    }
}
