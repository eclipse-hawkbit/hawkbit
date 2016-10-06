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

import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetUpdateEvent;

/**
 * EventHolder for {@link DistributionSetUpdateEvent}s.
 *
 */
public class DistributionSetUpdatedEventHolder implements EventHolder<DistributionSetUpdateEvent> {
    private final List<DistributionSetUpdateEvent> events;

    public DistributionSetUpdatedEventHolder(final List<DistributionSetUpdateEvent> events) {
        this.events = events;
    }

    @Override
    public List<DistributionSetUpdateEvent> getEvents() {
        return events;
    }

}
