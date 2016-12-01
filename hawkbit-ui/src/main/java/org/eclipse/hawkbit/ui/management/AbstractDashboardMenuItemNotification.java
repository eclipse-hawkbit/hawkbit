/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;

import com.vaadin.ui.Label;

/**
 * Contains the menu items' Label for the notification display.
 */
public abstract class AbstractDashboardMenuItemNotification implements DashboardMenuItem {

    private static final long serialVersionUID = 1L;

    private final Label notificationsLabel = new Label();

    @Override
    public void setNotificationUnreadValue(final int notificationUnread) {
        notificationsLabel.setValue(String.valueOf(notificationUnread));
        notificationsLabel.setVisible(notificationUnread > 0);

    }

    @Override
    public Label getNotificationUnreadLabel() {
        return notificationsLabel;
    }

}
