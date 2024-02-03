/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 *
 * Defines the remote event of deleting a {@link TargetType}.
 */
public class TargetTypeDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public TargetTypeDeletedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor for json serialization.
     *
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity class
     * @param applicationId
     *            the origin application id
     */
    public TargetTypeDeletedEvent(final String tenant, final Long entityId,
            final Class<? extends TenantAwareBaseEntity> entityClass, final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
    }
}