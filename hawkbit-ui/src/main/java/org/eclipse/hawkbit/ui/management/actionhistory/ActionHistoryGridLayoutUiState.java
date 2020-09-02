/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
