/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.EntityEvent;
import org.eclipse.hawkbit.repository.event.remote.TenantAwareDistributedEvent;
import org.eclipse.hawkbit.repository.event.remote.json.GenericEventEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An abstract definition class for {@link EntityEvent} for some object
 *
 * @param <E>
 *            the type of the entity
 */
public class BaseEntityEvent<E, I> extends TenantAwareDistributedEvent implements EntityEvent<I> {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private GenericEventEntity<I> entitySource;

    @JsonIgnore
    private transient E entity;

    /**
     * Constructor for json serialization.
     * 
     * @param entitySource
     *            the entity source within the json entity information
     * @param tenant
     *            the tenant
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected BaseEntityEvent(@JsonProperty("entitySource") final GenericEventEntity<I> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
        this.entitySource = entitySource;
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the id
     * @param entity
     *            the entity
     * @param the
     *            origin application id
     */
    protected BaseEntityEvent(final String tenant, final I entityId, final E entity, final String applicationId) {
        this(new GenericEventEntity<I>(entityId, entity.getClass().getName()), tenant, applicationId);
        this.entity = entity;
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity class
     * @param applicationId
     *            the origin application id
     */
    protected BaseEntityEvent(final String tenant, final I entityId, final Class<?> entityClass,
            final String applicationId) {
        this(new GenericEventEntity<I>(entityId, entityClass.getName()), tenant, applicationId);
    }

    @Override
    public E getEntity() {
        if (entity == null) {
            System.out.println("Remote Event loading");
            // Idee entity manager zum laden verwenden entitySource.getId +
            // entitySource.getTenant

        }
        return entity;
    }

    @Override
    @JsonIgnore
    public <T> T getEntity(final Class<T> entityClass) {
        return entityClass.cast(entity);
    }

    protected void setEntity(final E entity) {
        this.entity = entity;
    }

    public GenericEventEntity<I> getEntitySource() {
        return entitySource;
    }

    @Override
    @JsonIgnore
    public I getEntityId() {
        return entitySource.getGenericId();
    }

}
