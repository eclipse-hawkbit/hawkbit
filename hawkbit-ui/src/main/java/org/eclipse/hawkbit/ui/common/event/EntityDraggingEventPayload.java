/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
