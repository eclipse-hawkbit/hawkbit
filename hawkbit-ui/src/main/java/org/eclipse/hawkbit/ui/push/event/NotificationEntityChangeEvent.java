/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.push.event;

import org.eclipse.hawkbit.ui.common.table.AbstractTable;

/**
 * A event which fire is firen when a new notification is available.
 */
public class NotificationEntityChangeEvent {

    /**
     * 
     * The different event types.
     */
    public enum EventType {
        ENITY_ADDED, ENTITY_DELETED;
    }

    private final AbstractTable<?, ?> sender;
    private final EventType type;
    private final String message;
    private int unreadNotificationSize;

    /**
     * Constructor.
     * 
     * @param sender
     *            the table which send the event
     * @param type
     *            the event type
     * @param message
     *            the message
     * @param unreadNotificationSize
     *            how many different notifications
     */
    public NotificationEntityChangeEvent(final AbstractTable<?, ?> sender, final EventType type, final String message,
            final int unreadNotificationSize) {
        this.sender = sender;
        this.type = type;
        this.message = message;
        this.unreadNotificationSize = unreadNotificationSize;
    }

    public AbstractTable<?, ?> getSender() {
        return sender;
    }

    public EventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getUnreadNotificationSize() {
        return unreadNotificationSize;
    }

    public void setUnreadNotificationSize(final int unreadNotificationSize) {
        this.unreadNotificationSize = unreadNotificationSize;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sender == null) ? 0 : sender.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NotificationEntityChangeEvent)) {
            return false;
        }
        final NotificationEntityChangeEvent other = (NotificationEntityChangeEvent) obj;
        if (sender == null) {
            if (other.sender != null) {
                return false;
            }
        } else if (!sender.equals(other.sender)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

}
