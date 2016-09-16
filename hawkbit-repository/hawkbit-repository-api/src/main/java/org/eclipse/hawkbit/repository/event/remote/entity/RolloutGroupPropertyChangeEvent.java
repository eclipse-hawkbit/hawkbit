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

import org.eclipse.hawkbit.repository.model.RolloutGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the remote event of {@link RolloutGroup} proeprty changes.
 */
public class RolloutGroupPropertyChangeEvent extends BasePropertyChangeEvent<RolloutGroup> {

    private static final long serialVersionUID = 4026477044419472686L;

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
    protected RolloutGroupPropertyChangeEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("entityId") final Long entityId,
            @JsonProperty("entityClassName") final String entityClassName,
            @JsonProperty("changeSetValues") final Map<String, PropertyChange> changeSetValues,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, entityId, entityClassName, changeSetValues, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param entity
     *            the entity
     * @param changeSetValues
     *            the changeSetValues
     * @param applicationId
     *            the origin application id
     */
    public RolloutGroupPropertyChangeEvent(final RolloutGroup entity, final Map<String, PropertyChange> changeSetValues,
            final String applicationId) {
        super(entity, changeSetValues, applicationId);
    }

}
