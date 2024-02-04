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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.entity.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

import java.io.Serial;

/**
 * Defines the remote event of deleting a {@link Target}.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class TargetDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    private String controllerId;
    private String targetAddress;

    /**
     * Default constructor.
     */
    public TargetDeletedEvent() {
        // for serialization libs like jackson
    }

    /**
     *
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param controllerId
     *            the controllerId of the target
     * @param targetAddress
     *            the target address
     * @param entityClass
     *            the entity class
     * @param applicationId
     *            the origin application id
     */
    public TargetDeletedEvent(final String tenant, final Long entityId, final String controllerId,
            final String targetAddress, final Class<? extends TenantAwareBaseEntity> entityClass,
            final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
        this.controllerId = controllerId;
        this.targetAddress = targetAddress;
    }
}