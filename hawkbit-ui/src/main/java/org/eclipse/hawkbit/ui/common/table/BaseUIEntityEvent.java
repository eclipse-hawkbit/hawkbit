/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Event to represent add, update or delete.
 *
 */
public class BaseUIEntityEvent<T extends BaseEntity> {

    private final BaseEntityEventType eventType;

    private final T entity;

    /**
     * Base entity event
     * 
     * @param eventType
     *            the event type
     * @param entity
     *            the entity reference
     */
    public BaseUIEntityEvent(final BaseEntityEventType eventType, final T entity) {
        this.eventType = eventType;
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }

    public BaseEntityEventType getEventType() {
        return eventType;
    }

}
