/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

/**
 * Proxy for the Rollout view of system config window
 */
public class ProxySystemConfigRollout extends ProxySystemConfigWindow {
    private static final long serialVersionUID = 1L;

    private boolean rolloutApproval;

    /**
     * Flag that indicates if the rolloutApproval option is enabled.
     *
     * @return <code>true</code> if the rolloutApproval is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isRolloutApproval() {
        return rolloutApproval;
    }

    /**
     * Sets the flag that indicates if the rolloutApproval option is enabled.
     *
     * @param rolloutApproval
     *            <code>true</code> if the rolloutApproval is enabled, otherwise
     *            <code>false</code>
     */
    public void setRolloutApproval(final boolean rolloutApproval) {
        this.rolloutApproval = rolloutApproval;
    }
}
