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

import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;

/**
 * EventHolder for {@link TargetTagDeletedEvent}s.
 *
 */
public class TargetTagDeletedEventContainer implements EventContainer<TargetTagDeletedEvent> {
    private static final String I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE = "target.tag.deleted.event.container.notifcation.message";
    private final List<TargetTagDeletedEvent> events;

    TargetTagDeletedEventContainer(final List<TargetTagDeletedEvent> events) {
        this.events = events;
    }

    @Override
    public List<TargetTagDeletedEvent> getEvents() {
        return events;
    }

    @Override
    public String getUnreadNotificationMessageKey() {
        return I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE;
    }

}
