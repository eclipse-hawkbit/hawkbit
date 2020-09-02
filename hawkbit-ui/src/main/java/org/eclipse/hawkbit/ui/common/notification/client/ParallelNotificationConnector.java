/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.notification.client;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.ui.common.notification.ParallelNotification;

import com.google.gwt.dom.client.Style.Unit;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.notification.NotificationConnector;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.notification.NotificationServerRpc;
import com.vaadin.shared.ui.notification.NotificationState;

/**
 * Connector for {@link ParallelNotification} that holds all active
 * notifications of the current UI and positions them so they do not overlap.
 */
@Connect(value = ParallelNotification.class)
public class ParallelNotificationConnector extends NotificationConnector {
    private static final long serialVersionUID = 1L;

    // offset between the stacked Notifications in px
    private static final int OFFSET = 5;

    private static final Map<VNotification, Integer> notifications = new HashMap<>();
    private transient VNotification notification;

    @Override
    protected void extend(final ServerConnector target) {
        final NotificationState state = getState();
        notification = VNotification.showNotification(target.getConnection(), state.caption, state.description,
                state.htmlContentAllowed, getResourceUrl("icon"), state.styleName, state.position, state.delay);

        notification.addCloseHandler(event -> {
            notificationRemoved(notification);

            if (getParent() == null) {
                return;
            }

            final NotificationServerRpc rpc = getRpcProxy(NotificationServerRpc.class);
            rpc.closed();

            notification = null;
        });

        final int totalNotificationHeight = notifications.values().stream().reduce(0, Integer::sum);
        setMarginBottom(notification, totalNotificationHeight);

        notifications.put(notification, notification.getOffsetHeight() + OFFSET);
    }

    private static void notificationRemoved(final VNotification notificationRemoved) {
        final double marginOfRemoved = getMarginBottom(notificationRemoved);
        final Integer heightOfRemoved = notifications.remove(notificationRemoved);

        moveNotificationsAboveBy(marginOfRemoved, heightOfRemoved);
    }

    private static double getMarginBottom(final VNotification notification) {
        try {
            return Double.valueOf(
                    notification.getElement().getStyle().getMarginBottom().trim().toLowerCase().replace("px", ""));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private static void moveNotificationsAboveBy(final double marginOfRemoved, final Integer heightOfRemoved) {
        if (heightOfRemoved == null) {
            return;
        }

        notifications.keySet().forEach(n -> {
            final double margin = getMarginBottom(n);
            if (margin > marginOfRemoved) {
                setMarginBottom(n, margin - heightOfRemoved);
            }
        });
    }

    private static void setMarginBottom(final VNotification notification, final double margin) {
        notification.getElement().getStyle().setMarginBottom(margin, Unit.PX);
    }

    @Override
    public void onUnregister() {
        super.onUnregister();

        if (notification != null) {
            notification.hide();
            notification = null;
        }
    }
}