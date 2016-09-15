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
import org.eclipse.hawkbit.repository.model.DistributionSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractBaseEntityEvent} for update a
 * {@link DistributionSet}.
 *
 */
public class DistributionSetUpdateEvent extends TenantAwareBaseEntityEvent<DistributionSet> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected DistributionSetUpdateEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
    }

    /**
     * Constructor
     * 
     * @param ds
     *            Distribution Set
     */
    public DistributionSetUpdateEvent(final DistributionSet ds, final String applicationId) {
        super(ds, applicationId);
    }
}
