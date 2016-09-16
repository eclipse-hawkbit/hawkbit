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

import org.eclipse.hawkbit.repository.model.Action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the the remote event of {@link Action} property changes.
 */
public class ActionPropertyChangeEvent extends BasePropertyChangeEvent<Action> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClassName
     *            the entity entityClassName
     * @param changeSetValues
     *            the changeSetValues
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected ActionPropertyChangeEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClass") final Class<? extends Action> entityClass,
            @JsonProperty("changeSetValues") final Map<String, PropertyChange> changeSetValues,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClass, changeSetValues, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the entity
     * @param changeSetValues
     *            the changes
     * @param applicationId
     *            the origin application id
     */
    public ActionPropertyChangeEvent(final Action entity, final Map<String, PropertyChange> changeSetValues,
            final String applicationId) {
        super(entity, changeSetValues, applicationId);
    }

}
