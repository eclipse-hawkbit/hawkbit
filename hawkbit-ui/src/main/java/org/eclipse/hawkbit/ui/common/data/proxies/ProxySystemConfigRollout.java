/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
