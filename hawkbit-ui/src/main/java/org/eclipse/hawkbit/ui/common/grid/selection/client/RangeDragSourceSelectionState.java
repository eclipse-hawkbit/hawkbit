/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection.client;

import org.eclipse.hawkbit.ui.common.grid.selection.RangeSelectionGridDragSource;

import com.vaadin.shared.ui.grid.GridDragSourceState;

/**
 * State class containing parameters for {@link RangeSelectionGridDragSource}.
 */
public class RangeDragSourceSelectionState extends GridDragSourceState {
    private static final long serialVersionUID = 1L;

    private int selectionCount;

    /**
     * Gets the total selected items
     *
     * @return selectionCount
     */
    public int getSelectionCount() {
        return selectionCount;
    }

    /**
     * Sets the total selected items
     *
     * @param selectionCount
 *              Number of selected items
     */
    public void setSelectionCount(final int selectionCount) {
        this.selectionCount = selectionCount;
    }
}
