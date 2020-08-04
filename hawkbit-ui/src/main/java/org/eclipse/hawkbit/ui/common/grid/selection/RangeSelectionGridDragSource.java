/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.grid.selection;

import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.selection.client.RangeDragSourceSelectionState;

import com.vaadin.shared.Registration;
import com.vaadin.ui.components.grid.GridDragSource;

/**
 * Extends {@link GridDragSource} to offer the number of currently selected
 * items to the client side
 *
 * @param <T>
 *            item type
 */
public class RangeSelectionGridDragSource<T extends ProxyIdentifiableEntity> extends GridDragSource<T> {
    private static final long serialVersionUID = 1L;

    private final Registration selectionCountListenerRegistration;

    /**
     * Constructor
     * 
     * @param target
     *            Grid to be extended.
     */
    public RangeSelectionGridDragSource(final AbstractGrid<T, ?> target) {
        super(target);

        if (target.hasSelectionSupport()) {
            selectionCountListenerRegistration = target.getSelectionModel()
                    .addSelectionListener(event -> getState().setSelectionCount(event.getAllSelectedItems().size()));
        } else {
            selectionCountListenerRegistration = null;
        }
    }

    @Override
    protected RangeDragSourceSelectionState getState() {
        return (RangeDragSourceSelectionState) super.getState();
    }

    @Override
    protected RangeDragSourceSelectionState getState(final boolean markAsDirty) {
        return (RangeDragSourceSelectionState) super.getState(markAsDirty);
    }

    @Override
    public void remove() {
        if (selectionCountListenerRegistration != null) {
            selectionCountListenerRegistration.remove();
        }

        super.remove();
    }
}
