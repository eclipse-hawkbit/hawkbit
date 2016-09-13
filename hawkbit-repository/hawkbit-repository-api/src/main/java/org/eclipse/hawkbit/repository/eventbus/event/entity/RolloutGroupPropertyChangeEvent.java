/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.entity;

import java.util.Map;

import org.eclipse.hawkbit.repository.eventbus.event.entity.GenericEventEntity.PropertyChange;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link RolloutGroup}.
 */
public class RolloutGroupPropertyChangeEvent extends BasePropertyChangeEvent<RolloutGroup> {

    private static final long serialVersionUID = 4026477044419472686L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected RolloutGroupPropertyChangeEvent(
            @JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    public RolloutGroupPropertyChangeEvent(final RolloutGroup entity, final Map<String, PropertyChange> changeSetValues,
            final String originService) {
        super(entity, changeSetValues, originService);
    }

    /**
     * TODO REMOVE Constructor
     */
    public RolloutGroupPropertyChangeEvent(final RolloutGroup entity,
            final Map<String, PropertyChange> changeSetValues) {
        super(entity, changeSetValues, "REMOVE");
    }
}
