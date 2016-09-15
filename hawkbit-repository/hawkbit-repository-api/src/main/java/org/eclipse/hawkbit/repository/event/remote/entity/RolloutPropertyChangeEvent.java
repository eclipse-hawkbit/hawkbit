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
import org.eclipse.hawkbit.repository.model.Rollout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link Rollout}.
 */
public class RolloutPropertyChangeEvent extends BasePropertyChangeEvent<Rollout> {
    private static final long serialVersionUID = 1056221355466373514L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected RolloutPropertyChangeEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant, @JsonProperty("originService") final String applicationId) {
        super(entitySource, tenant, applicationId);
    }

    public RolloutPropertyChangeEvent(final Rollout entity, final Map<String, PropertyChange> changeSetValues,
            final String applicationId) {
        super(entity, changeSetValues, applicationId);
    }

}
