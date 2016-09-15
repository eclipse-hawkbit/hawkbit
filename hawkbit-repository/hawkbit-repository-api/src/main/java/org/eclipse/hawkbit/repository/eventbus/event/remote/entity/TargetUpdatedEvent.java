/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.remote.entity;

import org.eclipse.hawkbit.repository.eventbus.event.remote.json.GenericEventEntity;
import org.eclipse.hawkbit.repository.model.Target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractBaseEntityEvent} of updating a {@link Target}.
 *
 */
public class TargetUpdatedEvent extends TenantAwareBaseEntityEvent<Target> {

    private static final long serialVersionUID = 5665118668865832477L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected TargetUpdatedEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant) {
        super(entitySource, tenant);
    }

    /**
     * Constructor
     * 
     * @param baseEntity
     *            Target entity
     */
    public TargetUpdatedEvent(final Target baseEntity, final String originService) {
        super(baseEntity, originService);
    }

}
