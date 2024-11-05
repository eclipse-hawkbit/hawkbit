/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Collection;

import lombok.Data;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Event that is published when a rollout is stopped due to invalidation of a
 * {@link DistributionSet}.
 */
@Data
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
     * @param tenant the tenant
     * @param entityId the entity id
     * @param entityClass the entity class
     * @param applicationId the origin application id
     */
    public RolloutStoppedEvent(final String tenant, final String applicationId, final long rolloutId,
            final Collection<Long> rolloutGroupIds) {
        super(rolloutId, tenant, applicationId);
        this.rolloutId = rolloutId;
        this.rolloutGroupIds = rolloutGroupIds;
    }
}