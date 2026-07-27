/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serial;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Defines the remote event of deleting an {@link AutoAssignment}.
 */
@NoArgsConstructor
public class AutoAssignmentDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    public AutoAssignmentDeletedEvent(final String tenant, final Long entityId, final Class<? extends TenantAwareBaseEntity> entityClass) {
        super(tenant, entityId, entityClass);
    }
}
