/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.Action;

/**
 * Defines the {@link AbstractBaseEntityEvent} of creating a new {@link Action}.
 */
public class ActionCreatedEvent extends AbstractBaseEntityEvent<Action> {
    private static final long serialVersionUID = 181780358321768629L;

    /**
     * @param action
     */
    public ActionCreatedEvent(final Action action) {
        super(action);
    }

}
