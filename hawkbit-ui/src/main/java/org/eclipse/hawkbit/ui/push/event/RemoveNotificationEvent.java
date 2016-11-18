/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.push.event;

import org.eclipse.hawkbit.ui.common.table.AbstractTable;

/**
 * Event which is publish when a notification should removed.
 */
public class RemoveNotificationEvent {

    private final AbstractTable<?, ?> sender;

    /**
     * Constructor.
     * 
     * @param sender
     *            the table which sends the event
     */
    public RemoveNotificationEvent(final AbstractTable<?, ?> sender) {
        this.sender = sender;
    }

    public AbstractTable<?, ?> getSender() {
        return sender;
    }

}
