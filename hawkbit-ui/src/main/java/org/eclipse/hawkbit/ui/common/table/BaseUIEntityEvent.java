/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteIdEvent;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TenantAwareEvent to represent add, update or delete.
 *
 * * @param <T> entity class
 */
public class BaseUIEntityEvent<T extends BaseEntity> {

    private static final Logger LOG = LoggerFactory.getLogger(BaseUIEntityEvent.class);

    private final BaseEntityEventType eventType;

    private T entity;

    private final Collection<Long> entityIds;

    private Class<?> entityClass;

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
        entityIds = new ArrayList<>();
        if (entity != null) {
            entityIds.add(entity.getId());
            this.entityClass = entity.getClass();
        }
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

    /**
     * Check if the remove event is the same this ui event. Then maybe you can
     * skip the remote event because is already executed.
     * 
     * @param tenantAwareEvent
     *            the remote event
     * @return <true> match <false> not match
     */
    public boolean matchRemoteEvent(final TenantAwareEvent tenantAwareEvent) {
        if (!(tenantAwareEvent instanceof RemoteIdEvent) || entityClass == null || entityIds == null) {
            return false;
        }
        final RemoteIdEvent remoteIdEvent = (RemoteIdEvent) tenantAwareEvent;
        try {
            final Class<?> remoteEntityClass = ClassUtils.getClass(remoteIdEvent.getEntityClass());
            return entityClass.isAssignableFrom(remoteEntityClass) && entityIds.contains(remoteIdEvent.getEntityId());
        } catch (final ClassNotFoundException e) {
            LOG.error("Class not found", e);
            return false;
        }

    }

}
