/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Label;

/**
 * Contains the menu items' Label for the notification display.
 */
public abstract class AbstractDashboardMenuItemNotification implements DashboardMenuItem {

    private static final long serialVersionUID = 1L;

    private final Label notificationsLabel = new Label();

    private final VaadinMessageSource i18n;

    protected AbstractDashboardMenuItemNotification(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    public void setNotificationUnreadValue(final AtomicInteger notificationUnread) {
        notificationsLabel.setValue(String.valueOf(notificationUnread.get()));
        notificationsLabel.setVisible(notificationUnread.get() > 0);

    }

    @Override
    public Label getNotificationUnreadLabel() {
        return notificationsLabel;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }
}
