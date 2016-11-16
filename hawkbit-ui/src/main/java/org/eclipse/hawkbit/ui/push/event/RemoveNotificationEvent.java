/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.push.event;

import org.eclipse.hawkbit.ui.common.table.AbstractTable;

/**
 *
 */
public class RemoveNotificationEvent {

    private final AbstractTable<?, ?> sender;

    public RemoveNotificationEvent(final AbstractTable<?, ?> sender) {
        this.sender = sender;
    }

    public AbstractTable<?, ?> getSender() {
        return sender;
    }

}
