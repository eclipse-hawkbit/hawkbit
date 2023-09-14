/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
