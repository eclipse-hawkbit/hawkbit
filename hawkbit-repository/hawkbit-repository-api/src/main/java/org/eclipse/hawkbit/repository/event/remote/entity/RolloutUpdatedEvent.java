/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.event.entitiy.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * Defines the remote event of updated a {@link Rollout}.
 */
public class RolloutUpdatedEvent extends RemoteEntityEvent<Rollout> implements EntityUpdatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public RolloutUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor
     * 
     * @param rollout
     *            the updated rollout
     * @param applicationId
     *            the origin application id
     */
    public RolloutUpdatedEvent(final Rollout rollout, final String applicationId) {
        super(rollout, applicationId);
    }

}
