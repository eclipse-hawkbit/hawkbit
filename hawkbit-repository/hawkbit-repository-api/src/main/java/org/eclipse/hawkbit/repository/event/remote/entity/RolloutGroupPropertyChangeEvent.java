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
     * @param entitySource
     *            the entity source within the json entity information
     * @param tenant
     *            the tenant
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    protected RolloutGroupPropertyChangeEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
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
