/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.push.event;

import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.RemoteIdEvent;
import org.eclipse.hawkbit.repository.model.Rollout;

/**
 * TenantAwareEvent declaration for the UI to notify the UI that a rollout has
 * been changed.
 * 
 */
public class RolloutChangedEvent extends RemoteIdEvent implements EntityUpdatedEvent {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public RolloutChangedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor for json serialization.
     * 
     * @param entityId
     *            the entity Id
     * @param tenant
     *            the tenant
     */
    public RolloutChangedEvent(final String tenant, final Long entityId) {
        // application id is not needed, because we compose the event ourselves
        super(entityId, tenant, Rollout.class, null);
    }
}
