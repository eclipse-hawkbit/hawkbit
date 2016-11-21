/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * TenantAwareEvent to represent add, update or delete.
 *
 * * @param <T> entity class
 */
public class BaseUIEntityEvent<T extends BaseEntity> {

    private final BaseEntityEventType eventType;

    private T entity;

    private final Collection<Long> entityIds;

    private final Class<?> entityClass;

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
        this.entityIds = Arrays.asList(entity.getId());
        this.entityClass = entity.getClass();
    }

    /**
     * Delete entity event.
     * 
     * @param entityIds
     *            entities which will be deleted
     * @param entityClass
     *            the entityClass
     */
    public BaseUIEntityEvent(final Collection<Long> entityIds, final Class<T> entityClass) {
        this.eventType = BaseEntityEventType.REMOVE_ENTITIES;
        this.entityIds = entityIds;
        this.entityClass = entityClass;
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

    public boolean matchRemoteEvent(final TenantAwareEvent tenantAwareEvent) {
        if (!(tenantAwareEvent instanceof RemoteEntityEvent)) {
            return false;
        }
        final RemoteEntityEvent<?> remoteEntityEvent = (RemoteEntityEvent<?>) tenantAwareEvent;
        try {
            final Class<?> remoteEntityClass = ClassUtils.getClass(remoteEntityEvent.getEntityClass());
            return entityClass.isAssignableFrom(remoteEntityClass)
                    && entityIds.contains(remoteEntityEvent.getEntityId());
        } catch (final ClassNotFoundException e) {
            return false;
        }

    }

}
