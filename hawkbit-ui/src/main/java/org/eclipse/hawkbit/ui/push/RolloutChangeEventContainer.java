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

import org.eclipse.hawkbit.ui.push.event.RolloutChangeEvent;

/**
 * EventHolder for {@link RolloutChangeEvent}s.
 *
 */
public class RolloutChangeEventContainer implements EventContainer<RolloutChangeEvent> {
    private final List<RolloutChangeEvent> events;

    RolloutChangeEventContainer(final List<RolloutChangeEvent> events) {
        this.events = events;
    }

    @Override
    public List<RolloutChangeEvent> getEvents() {
        return events;
    }

}
