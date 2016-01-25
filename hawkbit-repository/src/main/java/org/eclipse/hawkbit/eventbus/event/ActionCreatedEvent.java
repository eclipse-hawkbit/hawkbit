/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.Action;

/**
 *
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
