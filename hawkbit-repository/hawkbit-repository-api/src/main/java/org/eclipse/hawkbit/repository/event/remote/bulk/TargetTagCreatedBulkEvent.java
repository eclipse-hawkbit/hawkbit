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

import org.eclipse.hawkbit.repository.model.TargetTag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A bulk event which contains one or many new target tags after creating.
 */
public class TargetTagCreatedBulkEvent extends BaseEntityBulkEvent<TargetTag> {

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
    protected TargetTagCreatedBulkEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entitiyIds") final List<Long> entitiyIds,
            @JsonProperty("entityClass") final Class<? extends TargetTag> entityClass,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entitiyIds, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param entities
     *            the new ds tags
     * @param applicationId
     *            the origin application id
     */
    public TargetTagCreatedBulkEvent(final String tenant, final List<TargetTag> entities, final String applicationId) {
        super(tenant, entities, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the new ds tag
     * 
     * @param applicationId
     *            the origin application id
     */
    public TargetTagCreatedBulkEvent(final TargetTag entity, final String applicationId) {
        super(entity, applicationId);
    }
}
