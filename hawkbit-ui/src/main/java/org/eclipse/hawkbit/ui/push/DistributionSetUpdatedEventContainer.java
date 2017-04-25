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

import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;

/**
 * EventHolder for {@link DistributionSetUpdatedEvent}s.
 *
 */
public class DistributionSetUpdatedEventContainer implements EventContainer<DistributionSetUpdatedEvent> {
    private final List<DistributionSetUpdatedEvent> events;

    DistributionSetUpdatedEventContainer(final List<DistributionSetUpdatedEvent> events) {
        this.events = events;
    }

    @Override
    public List<DistributionSetUpdatedEvent> getEvents() {
        return events;
    }

}
