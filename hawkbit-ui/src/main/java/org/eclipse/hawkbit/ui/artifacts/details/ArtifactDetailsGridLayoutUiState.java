/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
