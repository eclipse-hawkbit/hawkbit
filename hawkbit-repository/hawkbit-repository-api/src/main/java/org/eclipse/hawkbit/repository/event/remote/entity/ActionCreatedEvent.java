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
import org.eclipse.hawkbit.repository.event.entity.EntityCreatedEvent;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the remote event of creating a new {@link Action}.
 */
@NoArgsConstructor // for serialization libs like jackson
public class ActionCreatedEvent extends AbstractActionEvent implements EntityCreatedEvent {

    @Serial
    private static final long serialVersionUID = 2L;

    /**
     * Constructor
     *
     * @param action the created action
     * @param targetId targetId identifier (optional)
     * @param rolloutId rollout identifier (optional)
     * @param rolloutGroupId rollout group identifier (optional)
     * @param applicationId the origin application id
     */
    public ActionCreatedEvent(
            final Action action, final Long targetId, final Long rolloutId, final Long rolloutGroupId, final String applicationId) {
        super(action, targetId, rolloutId, rolloutGroupId, applicationId);
    }
}