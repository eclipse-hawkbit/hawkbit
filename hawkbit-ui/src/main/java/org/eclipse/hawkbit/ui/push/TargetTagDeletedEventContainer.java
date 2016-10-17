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

import org.eclipse.hawkbit.repository.eventbus.event.TargetTagDeletedEvent;

/**
 * EventHolder for {@link TargetTagDeletedEvent}s.
 *
 */
public class TargetTagDeletedEventContainer implements EventContainer<TargetTagDeletedEvent> {
    private final List<TargetTagDeletedEvent> events;

    TargetTagDeletedEventContainer(final List<TargetTagDeletedEvent> events) {
        this.events = events;
    }

    @Override
    public List<TargetTagDeletedEvent> getEvents() {
        return events;
    }

}
