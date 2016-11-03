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

import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;

/**
 * EventHolder for {@link DistributionSetDeletedEvent}s.
 *
 */
public class DistributionDeletedEventContainer implements EventContainer<DistributionSetDeletedEvent> {
    private final List<DistributionSetDeletedEvent> events;

    DistributionDeletedEventContainer(final List<DistributionSetDeletedEvent> events) {
        this.events = events;
    }

    @Override
    public List<DistributionSetDeletedEvent> getEvents() {
        return events;
    }

}
