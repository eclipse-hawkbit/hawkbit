/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TenantAwareEvent definition which is been published in case a rollout group
 * has been created for a specific rollout.
 *
 */
public class RolloutGroupCreatedEvent extends RemoteEntityEvent<RolloutGroup> {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Long rolloutId;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param rolloutId
     *            the ID of the rollout has been created
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected RolloutGroupCreatedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("rolloutId") final Long rolloutId, @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends RolloutGroup> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
        this.rolloutId = rolloutId;
    }

    /**
     * Constructor
     * 
     * @param rolloutGroup
     *            the updated rolloutGroup
     * @param applicationId
     *            the origin application id
     */
    public RolloutGroupCreatedEvent(final RolloutGroup rolloutGroup, final String applicationId) {
        super(rolloutGroup, applicationId);
        this.rolloutId = rolloutGroup.getRollout().getId();
    }

    public Long getRolloutId() {
        return rolloutId;
    }

}
