/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.state;

import java.io.Serializable;

/**
 * Hide layout ui state
 */
public class HidableLayoutUiState implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean hidden;

    /**
     * @return True if layout is hidden else false
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Sets the layout to hidden
     *
     * @param hidden
     *          boolean
     */
    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }
}
