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

import org.eclipse.hawkbit.repository.event.TenantAwareEvent;

/**
 * EventHolder beans contains a list of events that can be process by the UI in
 * batch like fashion.
 * 
 * @param <T>
 *            event type
 *
 */
@FunctionalInterface
public interface EventContainer<T extends TenantAwareEvent> {

    /**
     * @return list of contained events
     */
    List<T> getEvents();

    /**
     * 
     * @return the message for unread notification button. <null> means that no
     *         unread message is supported.
     */
    default String getUnreadNotificationMessageKey() {
        return null;
    }

}
