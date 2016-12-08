/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.menu;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

/**
 * Describe a menu entry for the Dashboard.
 * 
 *
 *
 */
public interface DashboardMenuItem extends Serializable {

    /**
     * Return the view name, which is navigate to after clicking the menu item.
     * 
     * @return the view name
     */
    String getViewName();

    /**
     * The icon for the menu item.
     * 
     * @return vaadin resource
     */
    Resource getDashboardIcon();

    /**
     * Return the dashboard caption.
     * 
     * @return the caption
     */
    String getDashboardCaption();

    /**
     * Return the menu item description.
     * 
     * @return the menu item description
     */
    String getDashboardCaptionLong();

    /**
     * Return the view permission to see the menu item. One permission must
     * match to see the menu item.
     * 
     * @return the list of permissions.
     */
    List<String> getPermissions();

    /**
     * Set the value of the
     * 
     * @param notificationUnread
     *            the unreadNotifciations
     */
    void setNotificationUnreadValue(AtomicInteger notificationUnread);

    /**
     * 
     * @return return the notification
     */
    Label getNotificationUnreadLabel();

}
