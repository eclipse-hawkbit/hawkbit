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

import org.eclipse.hawkbit.repository.event.local.DistributionSetTagAssigmentResultEvent;

/**
 * EventHolder for {@link DistributionSetTagAssigmentResultEvent}s.
 *
 */
public class DistributionSetTagAssignmentResultEventContainer
        implements EventContainer<DistributionSetTagAssigmentResultEvent> {
    private final List<DistributionSetTagAssigmentResultEvent> events;

    DistributionSetTagAssignmentResultEventContainer(final List<DistributionSetTagAssigmentResultEvent> events) {
        this.events = events;
    }

    @Override
    public List<DistributionSetTagAssigmentResultEvent> getEvents() {
        return events;
    }

}
