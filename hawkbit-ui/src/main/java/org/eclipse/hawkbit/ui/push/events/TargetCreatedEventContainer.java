/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.events;

import java.util.List;

import org.eclipse.hawkbit.repository.eventbus.event.TargetCreatedEvent;

/**
 * EventHolder for {@link TargetCreatedEvent}s.
 *
 */
public class TargetCreatedEventContainer implements EventContainer<TargetCreatedEvent> {
    private final List<TargetCreatedEvent> events;

    public TargetCreatedEventContainer(final List<TargetCreatedEvent> events) {
        this.events = events;
    }

    @Override
    public List<TargetCreatedEvent> getEvents() {
        return events;
    }

}
