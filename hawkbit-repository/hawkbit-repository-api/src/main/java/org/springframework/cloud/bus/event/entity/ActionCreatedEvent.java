/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event.entity;

import org.eclipse.hawkbit.repository.model.Action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines the {@link AbstractBaseEntityEvent} of creating a new {@link Action}.
 */
public class ActionCreatedEvent extends TenantAwareBaseEntityEvent<Action> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * Constructor for json serialization
     * 
     * @param entitySource
     *            the json infos
     */
    @JsonCreator
    protected ActionCreatedEvent(@JsonProperty("entitySource") final GenericEventEntity<Long> entitySource) {
        super(entitySource);
    }

    public ActionCreatedEvent(final Action action, final String originService) {
        super(action, originService);
    }

    /**
     * TODO REMOVE Constructor
     */
    public ActionCreatedEvent(final Action action) {
        super(action, "REMOVE");
    }

}
