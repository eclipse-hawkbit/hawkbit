/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.bulk;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A abstract typesafe bulkevent which contains all changed base entities.
 *
 * @param <E>
 *            the entity
 */
public class RemoteEntityBulkEvent<E extends TenantAwareBaseEntity> extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final List<Long> entitiyIds;

    @JsonProperty(required = true)
    private Class<? extends E> entityClass;

    @JsonIgnore
    private List<? extends E> entities;

    /**
     * Constructor for json serialization.
     *
     * @param tenant
     *            the tenant
     * @param entityIds
     *            the entity ids
     * @param entityClassName
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected RemoteEntityBulkEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entitiyIds") final List<Long> entitiyIds,
            @JsonProperty("entityClass") final Class<? extends E> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(entitiyIds, tenant, applicationId);
        this.entityClass = entityClass;
        this.entitiyIds = entitiyIds;
    }

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant
     * @param entities
     *            the new ds tags
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    protected RemoteEntityBulkEvent(final String tenant, final Class<? extends E> entityClass, final List<E> entities,
            final String applicationId) {
        this(tenant, entities.stream().map(entity -> entity.getId()).collect(Collectors.toList()), entityClass,
                applicationId);
        this.entities = entities;
    }

    /**
     * Constructor.
     *
     * @param entitiy
     *            the entity
     * @param applicationId
     *            the origin application id
     */
    @SuppressWarnings("unchecked")
    protected RemoteEntityBulkEvent(final E entitiy, final String applicationId) {
        this(entitiy.getTenant(), (Class<? extends E>) entitiy.getClass(), asList(entitiy), applicationId);
    }

    @JsonIgnore
    public List<E> getEntities() {
        if (CollectionUtils.isEmpty(entities)) {
            entities = EventEntityManagerHolder.getInstance().getEventEntityManager().findEntities(getTenant(),
                    entitiyIds, entityClass);
        }
        return unmodifiableList(entities);
    }

}
