/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.event.entitiy.EntityDeletedEvent;
import org.eclipse.hawkbit.repository.model.Target;

/**
 *
 * Defines the remote event of deleting a {@link Target}.
 */
public class TargetDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

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
            final String targetAddress, final String entityClass, final String applicationId) {
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
