/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push;

import java.util.List;

import org.eclipse.hawkbit.ui.push.event.RolloutGroupChangedEvent;

/**
 * EventHolder for {@link RolloutGroupChangedEvent}s.
 *
 */
public class RolloutGroupChangedEventContainer implements EventContainer<RolloutGroupChangedEvent> {
    private final List<RolloutGroupChangedEvent> events;

    RolloutGroupChangedEventContainer(final List<RolloutGroupChangedEvent> events) {
        this.events = events;
    }

    @Override
    public List<RolloutGroupChangedEvent> getEvents() {
        return events;
    }

}
