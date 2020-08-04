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

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.vaadin.client.connectors.grid.GridDragSourceConnector;
import com.vaadin.shared.ui.Connect;

/**
 * Client side Connector to that connects to
 * {@link RangeSelectionGridDragSource} and extends
 * {@link GridDragSourceConnector} to fix the wrong number of dragged items.
 * Displays the dragged item count according to the number of selected items on
 * server side.
 *
 */
@Connect(RangeSelectionGridDragSource.class)
public class RangeDragSourceConnector extends GridDragSourceConnector {
    private static final long serialVersionUID = 1L;

    private static final String STYLE_DRAG_DROP_COUNTER = "drag-drop-counter";

    @Override
    protected void setDragImage(final NativeEvent dragStartEvent) {
        final Element draggedRowElement = dragStartEvent.getEventTarget().cast();

        final int countSelected = getState().getSelectionCount();

        if (draggedRowElement.hasClassName("v-grid-row-selected")) {
            final Element counter = DOM.createSpan();
            counter.setInnerHTML(String.valueOf(countSelected));
            counter.setClassName(STYLE_DRAG_DROP_COUNTER);
            draggedRowElement.appendChild(counter);

            // removes the counter element from the dragged row after the drag
            // image is rendered
            AnimationScheduler.get().requestAnimationFrame(timestamp -> counter.removeFromParent(), draggedRowElement);
        }
        fixDragImageOffsetsForDesktop(dragStartEvent, draggedRowElement);
        fixDragImageTransformForMobile(draggedRowElement);
    }

    @Override
    public RangeDragSourceSelectionState getState() {
        return (RangeDragSourceSelectionState) super.getState();
    }

}
