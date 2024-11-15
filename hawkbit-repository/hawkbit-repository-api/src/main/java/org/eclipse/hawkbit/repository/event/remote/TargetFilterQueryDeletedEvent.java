/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Defines the remote event of deleting a {@link TargetFilterQuery}.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // for serialization libs like jackson
public class TargetFilterQueryDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * @param tenant the tenant
     * @param entityId the entity id
     * @param entityClass the entity class
     * @param applicationId the origin application id
     */
    public TargetFilterQueryDeletedEvent(final String tenant, final Long entityId,
            final Class<? extends TenantAwareBaseEntity> entityClass, final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
    }
}