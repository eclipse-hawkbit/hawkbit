/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * TenantAwareEvent definition which is been published in case a rollout group
 * has been created for a specific rollout or updated.
 *
 */
public abstract class AbstractRolloutGroupEvent extends RemoteEntityEvent<RolloutGroup> {
    private static final long serialVersionUID = 1L;

    private Long rolloutId;

    /**
     * Default constructor.
     */
    public AbstractRolloutGroupEvent() {
        // for serialization libs like jackson
    }

    public AbstractRolloutGroupEvent(final RolloutGroup rolloutGroup, final Long rolloutId,
            final String applicationId) {
        super(rolloutGroup, applicationId);
        this.rolloutId = rolloutId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

}
