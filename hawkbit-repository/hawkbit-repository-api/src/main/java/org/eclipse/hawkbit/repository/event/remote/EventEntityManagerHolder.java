/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A singleton bean which holds the event entity manager to have autowiring in the events.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public final class EventEntityManagerHolder {

    private static final EventEntityManagerHolder SINGLETON = new EventEntityManagerHolder();

    private EventEntityManager eventEntityManager;

    /**
     * @return the cache manager holder singleton instance
     */
    public static EventEntityManagerHolder getInstance() {
        return SINGLETON;
    }

    @Autowired // spring setter injection
    public void setEventEntityManager(final EventEntityManager eventEntityManager) {
        this.eventEntityManager = eventEntityManager;
    }

    /**
     * @return the eventEntityManager
     */
    public EventEntityManager getEventEntityManager() {
        return eventEntityManager;
    }
}