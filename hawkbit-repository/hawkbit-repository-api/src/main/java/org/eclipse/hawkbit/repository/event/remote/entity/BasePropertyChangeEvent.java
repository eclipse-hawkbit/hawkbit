/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.json.GenericEventEntity;
import org.eclipse.hawkbit.repository.event.remote.json.GenericEventEntity.PropertyChange;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base remote property change event.
 *
 * @param <E>
 *            the entity
 */
public class BasePropertyChangeEvent<E extends TenantAwareBaseEntity> extends TenantAwareBaseEntityEvent<E> {

    private static final long serialVersionUID = -3671601415138242311L;

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
    protected BasePropertyChangeEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the entity
     * @param changeSetValues
     *            the change values
     * @param applicationId
     *            the origin application id
     */
    protected BasePropertyChangeEvent(final E entity, final Map<String, PropertyChange> changeSetValues,
            final String applicationId) {
        super(entity, applicationId);
        getEntitySource().setChangeSetValues(changeSetValues);
    }

}
