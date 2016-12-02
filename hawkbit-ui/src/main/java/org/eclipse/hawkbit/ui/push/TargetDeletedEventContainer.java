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

import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;

/**
 * EventHolder for {@link TargetDeletedEvent}s.
 *
 */
public class TargetDeletedEventContainer implements EventContainer<TargetDeletedEvent> {
    private static final String I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE = "target.deleted.event.container.notifcation.message";
    private final List<TargetDeletedEvent> events;

    TargetDeletedEventContainer(final List<TargetDeletedEvent> events) {
        this.events = events;
    }

    @Override
    public List<TargetDeletedEvent> getEvents() {
        return events;
    }

    @Override
    public String getUnreadNotificationMessageKey() {
        return I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE;
    }

}
