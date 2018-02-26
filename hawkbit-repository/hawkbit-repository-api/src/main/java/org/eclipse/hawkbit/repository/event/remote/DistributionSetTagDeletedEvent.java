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
import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * Defines the the remote event of delete a {@link DistributionSetTag}.
 */
public class DistributionSetTagDeletedEvent extends RemoteIdEvent implements EntityDeletedEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DistributionSetTagDeletedEvent() {
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

    public DistributionSetTagDeletedEvent(final String tenant, final Long entityId, final String entityClass,
            final String applicationId) {
        super(entityId, tenant, entityClass, applicationId);
    }
}
