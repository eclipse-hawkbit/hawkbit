/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * An abstract definition class for {@link EntityEvent} for {@link TenantAwareBaseEntity}s,
 * which holds the {@link TenantAwareBaseEntity}.
 *
 *
 *
 * @param <E>
 *            the type of the {@link TenantAwareBaseEntity}
 */
public abstract class AbstractBaseEntityEvent<E extends TenantAwareBaseEntity> extends AbstractDistributedEvent
        implements EntityEvent {

    /**
    *
    */
    private static final long serialVersionUID = 1L;
    private final E entity;

    /**
     * @param baseEntity
     *            the entity which has been created or modified
     */
    public AbstractBaseEntityEvent(final E baseEntity) {
        super(baseEntity.getOptLockRevision(), baseEntity.getTenant());
        this.entity = baseEntity;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getEntity()
     */
    @Override
    public E getEntity() {
        return entity;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getEntity(java.lang
     * .Class)
     */
    @Override
    public <T> T getEntity(final Class<T> entityClass) {
        return entityClass.cast(entity);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getTenant()
     */
    @Override
    public String getTenant() {
        return entity.getTenant();
    }
}
