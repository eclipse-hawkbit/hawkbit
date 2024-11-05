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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the remote event of creating a new {@link Action}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = false)
public abstract class AbstractActionEvent extends RemoteEntityEvent<Action> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long targetId;
    private final Long rolloutId;
    private final Long rolloutGroupId;

    /**
     * Default constructor.
     */
    protected AbstractActionEvent() {
        // for serialization libs like jackson
        this.targetId = null;
        this.rolloutId = null;
        this.rolloutGroupId = null;
    }

    /**
     * Constructor
     *
     * @param action the created action
     * @param targetId targetId identifier (optional)
     * @param rolloutId rollout identifier (optional)
     * @param rolloutGroupId rollout group identifier (optional)
     * @param applicationId the origin application id
     */
    protected AbstractActionEvent(final Action action, final Long targetId, final Long rolloutId,
            final Long rolloutGroupId, final String applicationId) {
        super(action, applicationId);
        this.targetId = targetId;
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
    }
}