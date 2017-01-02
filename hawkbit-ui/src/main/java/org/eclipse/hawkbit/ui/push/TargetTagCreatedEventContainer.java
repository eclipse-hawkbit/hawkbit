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

import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;

/**
 * EventHolder for {@link TargetTagCreatedEvent}s.
 *
 */
public class TargetTagCreatedEventContainer implements EventContainer<TargetTagCreatedEvent> {
    private static final String I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE = "target.tag.created.event.container.notifcation.message";
    private final List<TargetTagCreatedEvent> events;

    TargetTagCreatedEventContainer(final List<TargetTagCreatedEvent> events) {
        this.events = events;
    }

    @Override
    public List<TargetTagCreatedEvent> getEvents() {
        return events;
    }

    @Override
    public String getUnreadNotificationMessageKey() {
        return I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE;
    }

}
