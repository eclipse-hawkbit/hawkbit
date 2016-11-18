/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * TenantAwareEvent to represent add, update or delete.
 *
 */
public class BaseUIEntityEvent<T extends BaseEntity> {

    private final BaseEntityEventType eventType;

    private T entity;

    private Collection<Long> entityIds;

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

    /**
     * Delete entity event.
     * 
     * @param entityIds
     *            entities which will be deleted
     */
    public BaseUIEntityEvent(final Collection<Long> entityIds) {
        this.eventType = BaseEntityEventType.REMOVE_ENTITIES;
        this.entityIds = entityIds;
    }

    public Collection<Long> getEntityIds() {
        return entityIds;
    }

    public T getEntity() {
        return entity;
    }

    public BaseEntityEventType getEventType() {
        return eventType;
    }

}
