/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.artifacts.details;

import java.io.Serializable;

/**
 * Display and set the current state of the artifacts detail layout
 *
 */
public class ArtifactDetailsGridLayoutUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean maximized;

    /**
     * @return current state artifact detail grid
     */
    public boolean isMaximized() {
        return maximized;
    }

    /**
     * @param maximized set artifact detail grid to true or false
     */
    public void setMaximized(final boolean maximized) {
        this.maximized = maximized;
    }
}
