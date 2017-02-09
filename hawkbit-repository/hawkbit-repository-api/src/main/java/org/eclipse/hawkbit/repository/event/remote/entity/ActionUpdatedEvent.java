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
 * Defines the remote event of updated a {@link Action}.
 */
public class ActionUpdatedEvent extends RemoteEntityEvent<Action> {
    private static final long serialVersionUID = 2L;

    private Long rolloutId;
    private Long rolloutGroupId;

    /**
     * Default constructor.
     */
    public ActionUpdatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor
     * 
     * @param action
     *            the updated action
     * @param applicationId
     *            the origin application id
     */
    public ActionUpdatedEvent(final Action action, final Long rolloutId, final Long rolloutGroupId,
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
