/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event.entity;

import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.cloud.bus.event.entity.GenericEventEntity.PropertyChange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractPropertyChangeEvent} of {@link Action}.
 */
public class ActionPropertyChangeEvent extends BasePropertyChangeEvent<Action> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected ActionPropertyChangeEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    public ActionPropertyChangeEvent(final Action entity, final Map<String, PropertyChange> changeSetValues,
            final String originService) {
        super(entity, changeSetValues, originService);
    }

    /**
     * TODO REMOVE Constructor
     */
    public ActionPropertyChangeEvent(final Action entity, final Map<String, PropertyChange> changeSetValues) {
        super(entity, changeSetValues, "REMOVE");
    }

}
