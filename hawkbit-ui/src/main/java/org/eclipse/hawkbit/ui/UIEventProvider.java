/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.eventbus.event.Event;

/**
 * The UI event provider hold all supported repository events which will
 * delegated to the UI. A event type can delegated as single event or bulk
 * event. Bulk event means, that all events from one type is collected by the
 * provider. The delegater and delegated as a list of this events.
 */
public interface UIEventProvider {

    /**
     * Return all supported repository single event types. All events which this
     * type are delegated to the UI as single event.
     * 
     * @return list of provided event types. Should not be null
     */
    default Set<Class<? extends Event>> getSingleEvents() {
        return Collections.emptySet();
    }

    /**
     * Return all supported repository bulk event types. All events which this
     * type are delegated to the UI as a list. This list contains all collected
     * events from one type.
     * 
     * @return list of provided bulk event types. Should not be null
     */
    default Set<Class<? extends Event>> getBulkEvents() {
        return Collections.emptySet();
    }

    /**
     * Return all filtered bulk event types by the given events. The default
     * maps the events by class.
     * 
     * @param allEvents
     *            the events
     * @return list of provided bulk event types which are filtered. Should not
     *         be null
     */
    default Set<Class<?>> getFilteredBulkEventsType(final List<Event> allEvents) {
        return allEvents.stream().map(Event::getClass).filter(getBulkEvents()::contains).collect(Collectors.toSet());
    }

}
