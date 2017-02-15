/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the remote event of creating a new {@link Action}.
 */
public abstract class AbstractActionEvent extends RemoteEntityEvent<Action> {
    private static final long serialVersionUID = 1L;

    private Long rolloutId;
    private Long rolloutGroupId;

    /**
     * Default constructor.
     */
    public AbstractActionEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor
     * 
     * @param action
     *            the created action
     * @param rolloutId
     *            rollout identifier (optional)
     * @param rolloutGroupId
     *            rollout group identifier (optional)
     * @param applicationId
     *            the origin application id
     */
    public AbstractActionEvent(final Action action, final Long rolloutId, final Long rolloutGroupId,
            final String applicationId) {
        super(action, applicationId);
        this.rolloutId = rolloutId;
        this.rolloutGroupId = rolloutGroupId;
    }

    public Long getRolloutId() {
        return rolloutId;
    }

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

}
