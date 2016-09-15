/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.remote.json.GenericEventEntity;
import org.eclipse.hawkbit.repository.model.Target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Defines the {@link AbstractBaseEntityEvent} of deleting a {@link Target}.
 */
public class TargetDeletedEvent extends BaseEntityEvent<Target, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected TargetDeletedEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
    }

    /**
     * @param tenant
     *            the tenant for this event
     * @param targetId
     *            the ID of the target which has been deleted
     */
    public TargetDeletedEvent(final String tenant, final Long targetId, final String applicationId) {
        super(tenant, targetId, Target.class, applicationId);
    }

}
