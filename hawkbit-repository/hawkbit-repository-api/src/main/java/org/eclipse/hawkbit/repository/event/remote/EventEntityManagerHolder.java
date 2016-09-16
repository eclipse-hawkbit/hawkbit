/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the event entity manager to have autowiring in
 * the events.
 *
 */
public final class EventEntityManagerHolder {

    private static final EventEntityManagerHolder SINGLETON = new EventEntityManagerHolder();

    @Autowired
    private EventEntityManager eventEntityManager;

    private EventEntityManagerHolder() {

    }

    /**
     * @return the cache manager holder singleton instance
     */
    public static EventEntityManagerHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return the eventEntityManager
     */
    public EventEntityManager getEventEntityManager() {
        return eventEntityManager;
    }
}
