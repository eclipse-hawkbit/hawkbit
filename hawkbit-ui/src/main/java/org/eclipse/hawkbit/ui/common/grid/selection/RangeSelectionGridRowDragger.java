/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.grid.selection;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;

import com.vaadin.ui.components.grid.GridDragSource;
import com.vaadin.ui.components.grid.GridRowDragger;

/**
 * {@link GridRowDragger} that uses {@link RangeSelectionGridDragSource} as
 * DragSource to fix the wrong item count displayed while dragging items that
 * are not displayed
 *
 * @param <T>
 *            item type
 */
public class RangeSelectionGridRowDragger<T extends ProxyIdentifiableEntity> extends GridRowDragger<T> {
    private static final long serialVersionUID = 1L;

    private final RangeSelectionGridDragSource<T> rangeSelectionGridDragSource;

    /**
     * Constructor
     * 
     * @param source
     *            Grid to be extended.
     */
    public RangeSelectionGridRowDragger(final AbstractGrid<T, ?> source) {
        super(source);
        super.getGridDragSource().remove();
        rangeSelectionGridDragSource = new RangeSelectionGridDragSource<>(source);
    }

    @Override
    public GridDragSource<T> getGridDragSource() {
        return rangeSelectionGridDragSource;
    }
}
