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

import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;

/**
 * EventHolder for {@link SoftwareModuleDeletedEvent}s.
 *
 */
public class SoftwareModuleDeletedEventContainer implements EventContainer<SoftwareModuleDeletedEvent> {
    private static final String I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE = "software.module.deleted.event.container.notifcation.message";
    private final List<SoftwareModuleDeletedEvent> events;

    SoftwareModuleDeletedEventContainer(final List<SoftwareModuleDeletedEvent> events) {
        this.events = events;
    }

    @Override
    public List<SoftwareModuleDeletedEvent> getEvents() {
        return events;
    }

    @Override
    public String getUnreadNotificationMessageKey() {
        return I18N_UNREAD_NOTIFICATION_UNREAD_MESSAGE;
    }

}
