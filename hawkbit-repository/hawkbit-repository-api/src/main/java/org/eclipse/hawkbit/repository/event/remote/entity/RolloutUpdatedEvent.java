/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Rollout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the remote event of updated a {@link Rollout}.
 */
public class RolloutUpdatedEvent extends RemoteEntityEvent<Rollout> {
    private static final long serialVersionUID = 1056221355466373514L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected RolloutUpdatedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends Rollout> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
    }

    /**
     * Constructor
     * 
     * @param rollout
     *            the updated rollout
     * @param applicationId
     *            the origin application id
     */
    public RolloutUpdatedEvent(final Rollout rollout, final String applicationId) {
        super(rollout, applicationId);
    }

}
