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
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 *
 * Defines the remote event of deleting a {@link TargetFilterQuery}.
 */
public class TargetFilterQueryDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    private static final long serialVersionUID = 2L;

    /**
     * Default constructor.
     */
    public TargetFilterQueryDeletedEvent() {
        // for serialization libs like jackson
    }

    /**
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
    public TargetFilterQueryDeletedEvent(final String tenant, final Long entityId, final String entityClass,
            final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
    }
}
