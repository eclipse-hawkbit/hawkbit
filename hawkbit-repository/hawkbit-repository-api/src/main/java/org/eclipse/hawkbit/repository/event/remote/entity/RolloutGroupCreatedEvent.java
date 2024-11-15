/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.io.Serial;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * TenantAwareEvent definition which is being published in case a rollout group
 * has been created for a specific rollout.
 */
@NoArgsConstructor(access = AccessLevel.PUBLIC) // for serialization libs like jackson
public class RolloutGroupCreatedEvent extends AbstractRolloutGroupEvent implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param rolloutGroup the updated rolloutGroup
     * @param rolloutId of the related rollout
     * @param applicationId the origin application id
     */
    public RolloutGroupCreatedEvent(final RolloutGroup rolloutGroup, final Long rolloutId, final String applicationId) {
        super(rolloutGroup, rolloutId, applicationId);
    }
}