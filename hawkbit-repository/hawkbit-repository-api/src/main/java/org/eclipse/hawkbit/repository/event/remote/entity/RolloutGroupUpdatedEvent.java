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

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.event.entity.EntityUpdatedEvent;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Defines the remote event of updated a {@link RolloutGroup}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class RolloutGroupUpdatedEvent extends AbstractRolloutGroupEvent implements EntityUpdatedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * Constructor
     *
     * @param rolloutGroup the updated rolloutGroup
     * @param rolloutId of the related rollout
     * @param applicationId the origin application id
     */
    public RolloutGroupUpdatedEvent(final RolloutGroup rolloutGroup, final Long rolloutId, final String applicationId) {
        super(rolloutGroup, rolloutId, applicationId);
    }
}