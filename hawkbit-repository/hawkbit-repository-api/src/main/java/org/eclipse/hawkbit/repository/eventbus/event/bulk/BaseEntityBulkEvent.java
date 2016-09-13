/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.bulk;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.eventbus.event.entity.BaseEntityEvent;
import org.eclipse.hawkbit.repository.eventbus.event.entity.GenericEventEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * A abstract typesafe bulkevent which contains all changed base entities.
 * 
 * @param <E>
 */
public abstract class BaseEntityBulkEvent<E extends TenantAwareBaseEntity>
        extends BaseEntityEvent<List<E>, List<Long>> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected BaseEntityBulkEvent(@JsonProperty("entitySource") final GenericEventEntity<List<Long>> entitySource) {
        super(entitySource);
    }

    protected BaseEntityBulkEvent(final String tenant, final List<E> entities, final Class<?> entityClass,
            final String originService) {
        super(tenant, entities.stream().map(entity -> entity.getId()).collect(Collectors.toList()), entityClass,
                originService);

    }

    protected BaseEntityBulkEvent(final E entitiy, final String originService) {
        this(entitiy.getTenant(), Arrays.asList(entitiy), entitiy.getClass(), originService);
    }

    @Override
    public List<E> getEntity() {
        if (getEntity() == null) {
            System.out.println("Remote Event loading");
            // Idee entity manager zum laden verwenden entitySource.getId +
            // entitySource.getTenant
            // setEntity(entity);
            // TODO Entitymanager findAll or someting like that
        }
        return super.getEntity();
    }

    public List<E> getEntities() {
        return getEntity();
    }

}
