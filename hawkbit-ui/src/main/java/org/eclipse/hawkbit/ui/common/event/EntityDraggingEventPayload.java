/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Payload containing information about an Drag and Drop event
 *
 */
public class EntityDraggingEventPayload {
    private final String sourceGridId;
    private final DraggingEventType draggingEventType;

    /**
     * Type of dragging event
     */
    public enum DraggingEventType {
        STARTED, STOPPED;
    }

    /**
     * Constructor
     * 
     * @param sourceGridId
     *            grid-ID of the grid the items are dragged from
     * @param draggingEventType
     *            type of dragging event
     */
    public EntityDraggingEventPayload(final String sourceGridId, final DraggingEventType draggingEventType) {
        this.sourceGridId = sourceGridId;
        this.draggingEventType = draggingEventType;
    }

    /**
     * @return grid-ID of the grid
     */
    public String getSourceGridId() {
        return sourceGridId;
    }

    /**
     * @return Dragging event type
     */
    public DraggingEventType getDraggingEventType() {
        return draggingEventType;
    }

}
