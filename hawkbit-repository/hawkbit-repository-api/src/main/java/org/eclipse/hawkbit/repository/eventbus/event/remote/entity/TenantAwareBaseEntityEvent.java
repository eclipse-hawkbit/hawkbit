/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.remote.entity;

import org.eclipse.hawkbit.eventbus.event.EntityEvent;
import org.eclipse.hawkbit.repository.eventbus.event.remote.json.GenericEventEntity;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An abstract definition class for {@link EntityEvent} for some object
 *
 * @param <E>
 *            the type of the entity
 */
public class TenantAwareBaseEntityEvent<E extends TenantAwareBaseEntity> extends BaseEntityEvent<E, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected TenantAwareBaseEntityEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant) {
        super(entitySource, tenant);
    }

    protected TenantAwareBaseEntityEvent(final E baseEntity, final String originService) {
        super(baseEntity.getTenant(), baseEntity.getId(), baseEntity.getClass(), originService);
    }
}
