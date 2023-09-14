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
