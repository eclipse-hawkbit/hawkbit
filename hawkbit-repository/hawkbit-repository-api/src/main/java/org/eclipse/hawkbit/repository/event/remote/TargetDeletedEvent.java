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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Defines the remote event of deleting a {@link Target}.
 */
@NoArgsConstructor // for serialization libs like jackson
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TargetDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    private String controllerId;
    private String targetAddress;

    public TargetDeletedEvent(
            final String tenant, final Long entityId, final Class<? extends TenantAwareBaseEntity> entityClass,
            final String controllerId, final String targetAddress) {
        super(tenant, entityId, entityClass);
        this.controllerId = controllerId;
        this.targetAddress = targetAddress;
    }
}