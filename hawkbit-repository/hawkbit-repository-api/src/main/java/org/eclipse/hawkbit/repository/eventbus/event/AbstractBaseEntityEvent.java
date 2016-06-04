/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.AbstractDistributedEvent;
import org.eclipse.hawkbit.eventbus.event.EntityEvent;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * An abstract definition class for {@link EntityEvent} for
 * {@link TenantAwareBaseEntity}s, which holds the {@link TenantAwareBaseEntity}
 * .
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

    @Override
    public E getEntity() {
        return entity;
    }

    @Override
    public <T> T getEntity(final Class<T> entityClass) {
        return entityClass.cast(entity);
    }

    @Override
    public String getTenant() {
        return entity.getTenant();
    }
}
