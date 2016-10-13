/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.remote.RemoteTenantAwareEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An base definition class for an event which contains an id.
 *
 */
public class RemoteIdEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Long entityId;

    /**
     * Constructor for json serialization.
     * 
     * @param entityId
     *            the entity Id
     * @param tenant
     *            the tenant
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected RemoteIdEvent(@JsonProperty("entityId") final Long entityId, @JsonProperty("tenant") final String tenant,
            @JsonProperty("originService") final String applicationId) {
        super(entityId, tenant, applicationId);
        this.entityId = entityId;
    }

    public Long getEntityId() {
        return entityId;
    }

}
