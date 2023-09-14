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
import org.eclipse.hawkbit.repository.model.Action;

/**
 * TenantAwareEvent declaration for the UI to notify the UI that a rollout has
 * been changed.
 * 
 *
 */
public class ActionChangedEvent extends RemoteIdEvent implements EntityUpdatedEvent, ParentIdAwareEvent {
    private static final long serialVersionUID = 1L;

    private final Long targetId;

    /**
     * Default constructor.
     */
    public ActionChangedEvent() {
        // for serialization libs like jackson
        this.targetId = null;
    }

    /**
     * Constructor for json serialization.
     *
     * @param tenant
     *            the tenant
     * @param targetId
     *            the target Id
     * @param entityId
     *            the entity Id
     */
    public ActionChangedEvent(final String tenant, final Long targetId, final Long entityId) {
        // application id is not needed, because we compose the event ourselves
        super(entityId, tenant, Action.class, null);

        this.targetId = targetId;
    }

    @Override
    public Long getParentEntityId() {
        return targetId;
    }
}
