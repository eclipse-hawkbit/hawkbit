/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Collection;

import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 *
 * Event that is published when a rollout is stopped due to invalidation of a
 * {@link DistributionSet}.
 */
public class RolloutStoppedEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private Collection<Long> rolloutGroupIds;
    private long rolloutId;

    /**
     * Default constructor.
     */
    public RolloutStoppedEvent() {
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
    public RolloutStoppedEvent(final String tenant, final String applicationId, final long rolloutId,
            final Collection<Long> rolloutGroupIds) {
        super(rolloutId, tenant, applicationId);
        this.rolloutId = rolloutId;
        this.rolloutGroupIds = rolloutGroupIds;
    }

    public Collection<Long> getRolloutGroupIds() {
        return rolloutGroupIds;
    }

    public void setRolloutGroupIds(final Collection<Long> rolloutGroupIds) {
        this.rolloutGroupIds = rolloutGroupIds;
    }

    public long getRolloutId() {
        return rolloutId;
    }

    public void setRolloutId(final long rolloutId) {
        this.rolloutId = rolloutId;
    }

}
