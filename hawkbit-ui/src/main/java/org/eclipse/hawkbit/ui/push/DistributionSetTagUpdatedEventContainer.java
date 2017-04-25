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

import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdatedEvent;

/**
 * EventHolder for {@link DistributionSetTagUpdatedEvent}s.
 *
 */
public class DistributionSetTagUpdatedEventContainer implements EventContainer<DistributionSetTagUpdatedEvent> {
    private static final String I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE = "distribution.set.tag.updated.event.container.notifcation.message";
    private final List<DistributionSetTagUpdatedEvent> events;

    DistributionSetTagUpdatedEventContainer(final List<DistributionSetTagUpdatedEvent> events) {
        this.events = events;
    }

    @Override
    public List<DistributionSetTagUpdatedEvent> getEvents() {
        return events;
    }

    @Override
    public String getUnreadNotificationMessageKey() {
        return I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE;
    }

}
