/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event.entity;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.cloud.bus.event.AbstractDistributedEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractDistributedEvent} for deletion of
 * {@link DistributionSet}.
 */
public class DistributionDeletedEvent extends BaseEntityEvent<DistributionSet, Long> {
    private static final long serialVersionUID = -3308850381757843098L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected DistributionDeletedEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    /**
     * @param tenant
     *            the tenant for this event
     * @param distributionId
     *            the ID of the distribution set which has been deleted
     */
    public DistributionDeletedEvent(final String tenant, final Long distributionId, final String originService) {
        super(tenant, distributionId, DistributionSet.class, originService);
    }

    /**
     * TODO REMOVE Constructor
     */
    public DistributionDeletedEvent(final String tenant, final Long distributionId) {
        super(tenant, distributionId, DistributionSet.class, "REMOVE");
    }

}
