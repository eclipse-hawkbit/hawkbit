/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the remote event for updating a {@link DistributionSet}.
 *
 */
public class DistributionSetUpdateEvent extends RemoteEntityEvent<DistributionSet> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    protected DistributionSetUpdateEvent(final String tenant, final Long entityId, final String entityClass,
            final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param ds
     *            Distribution Set
     * @param applicationId
     *            the origin application id
     */
    public DistributionSetUpdateEvent(final DistributionSet ds, final String applicationId) {
        super(ds, applicationId);
    }
}
