/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.event;

import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteIdEvent;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * TenantAwareEvent declaration for the UI to notify the UI that a rollout has
 * been changed.
 * 
 *
 */
public class RolloutGroupChangedEvent extends RemoteIdEvent implements EntityUpdatedEvent, ParentIdAwareEvent {
    private static final long serialVersionUID = 1L;

    private final Long rolloutId;

    /**
     * Default constructor.
     */
    public RolloutGroupChangedEvent() {
        // for serialization libs like jackson
        this.rolloutId = null;
    }

    /**
     * Constructor for json serialization.
     *
     * @param tenant
     *            the tenant
     * @param rolloutId
     *            the rollout Id
     * @param entityId
     *            the entity Id
     */
    public RolloutGroupChangedEvent(final String tenant, final Long rolloutId, final Long entityId) {
        // application id is not needed, because we compose the event ourselves
        super(entityId, tenant, RolloutGroup.class, null);

        this.rolloutId = rolloutId;
    }

    @Override
    public Long getParentEntityId() {
        return rolloutId;
    }
}
