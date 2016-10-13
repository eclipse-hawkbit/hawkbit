/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.EntityIdEvent;
import org.eclipse.hawkbit.repository.event.remote.TenantAwareDistributedEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An base definition class for {@link EntityIdEvent} for some object which has
 * an id.
 *
 */
public class BaseEntityIdEvent extends TenantAwareDistributedEvent implements EntityIdEvent<Long> {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Long entityId;

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
    protected BaseEntityIdEvent(@JsonProperty("entityId") final Long entityId,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entityId, tenant, applicationId);
        this.entityId = entityId;
    }

    @Override
    public Long getEntityId() {
        return entityId;
    }

}
