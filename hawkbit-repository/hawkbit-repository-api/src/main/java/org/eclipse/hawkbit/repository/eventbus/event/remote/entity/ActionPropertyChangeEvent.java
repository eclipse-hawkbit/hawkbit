/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event.remote.entity;

import java.util.Map;

import org.eclipse.hawkbit.repository.eventbus.event.remote.json.GenericEventEntity;
import org.eclipse.hawkbit.repository.eventbus.event.remote.json.GenericEventEntity.PropertyChange;
import org.eclipse.hawkbit.repository.model.Action;

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
    protected ActionPropertyChangeEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource,
            @JsonProperty("tenant") final String tenant) {
        super(entitySource, tenant);
    }

    public ActionPropertyChangeEvent(final Action entity, final Map<String, PropertyChange> changeSetValues,
            final String originService) {
        super(entity, changeSetValues, originService);
    }

}
