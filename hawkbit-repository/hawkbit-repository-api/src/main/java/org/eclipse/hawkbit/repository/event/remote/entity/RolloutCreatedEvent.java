/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entitiy.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Defines the remote event of creating a new {@link Rollout}.
 *
 */
public class RolloutCreatedEvent extends RemoteEntityEvent<Rollout> implements EntityCreatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public RolloutCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            the Rollout
     * @param applicationId
     *            the origin application id
     */
    public RolloutCreatedEvent(final Rollout baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
