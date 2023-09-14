/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.io.Serializable;

/**
 * Action history grid layout ui state
 */
public class ActionHistoryGridLayoutUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean maximized;

    /**
     * @return true if grid id maximized else false
     */
    public boolean isMaximized() {
        return maximized;
    }

    /**
     * Sets Maximize in grid
     *
     * @param maximized
     *          boolean
     */
    public void setMaximized(final boolean maximized) {
        this.maximized = maximized;
    }
}
