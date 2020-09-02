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
 * Payload event for selection changed
 *
 * @param <T>
 *          Generic type
 */
public class SelectionChangedEventPayload<T> extends EventLayoutViewAware {

    private final SelectionChangedEventType selectionChangedEventType;
    private final T entity;

    /**
     * Constructor for SelectionChangedEventPayload
     *
     * @param selectionChangedEventType
     *          SelectionChangedEventType
     * @param entity
     *          Generic type entity
     * @param layout
     *          EventLayout
     * @param view
     *          EventView
     */
    public SelectionChangedEventPayload(final SelectionChangedEventType selectionChangedEventType, final T entity,
            final EventLayout layout, final EventView view) {
        super(layout, view);

        this.selectionChangedEventType = selectionChangedEventType;
        this.entity = entity;
    }

    /**
     * @return Selection change event type
     */
    public SelectionChangedEventType getSelectionChangedEventType() {
        return selectionChangedEventType;
    }

    /**
     * @return Entity
     */
    public T getEntity() {
        return entity;
    }

    /**
     * Type of selection change event
     */
    public enum SelectionChangedEventType {
        ENTITY_SELECTED, ENTITY_DESELECTED;
    }
}
