/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.bulk;

import java.util.List;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * * A bulk event which contains one or many new ds tag after creating.
 */
public class DistributionSetTagCreatedBulkEvent extends BaseEntityBulkEvent<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

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
    protected DistributionSetTagCreatedBulkEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entitiyIds") final List<Long> entitiyIds,
            @JsonProperty("entityClass") final Class<? extends DistributionSetTag> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entitiyIds, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * 
     * @param entityClass
     *            the entity entityClassName
     * @param entities
     *            the new ds tags
     * @param applicationId
     *            the origin application id
     */
    public DistributionSetTagCreatedBulkEvent(final String tenant,
            final Class<? extends DistributionSetTag> entityClass, final List<DistributionSetTag> entities,
            final String applicationId) {
        super(tenant, entityClass, entities, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the new ds tag
     * @param applicationId
     *            the origin application id
     */
    public DistributionSetTagCreatedBulkEvent(final DistributionSetTag entity, final String applicationId) {
        super(entity, applicationId);
    }

}
