/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;

/**
 * Defines the remote event of triggering attribute updates of a {@link Target}.
 */
public class TargetAttributesRequestedEvent extends RemoteIdEvent {
    private static final long serialVersionUID = 1L;
    private String controllerId;
    private String targetAddress;

    /**
     * Default constructor.
     */
    public TargetAttributesRequestedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor json serialization
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
    public TargetAttributesRequestedEvent(final String tenant, final Long entityId, final String controllerId,
            final String targetAddress, final Class<? extends TenantAwareBaseEntity> entityClass,
            final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
        this.controllerId = controllerId;
        this.targetAddress = targetAddress;
    }

    public String getControllerId() {
        return controllerId;
    }

    public String getTargetAddress() {
        return targetAddress;
    }
}
