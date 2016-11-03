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
public class ActionCreatedEvent extends RemoteEntityEvent<Action> {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ActionCreatedEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor
     * 
     * @param action
     *            the created action
     * @param applicationId
     *            the origin application id
     */
    public ActionCreatedEvent(final Action action, final String applicationId) {
        super(action, applicationId);
    }

}
