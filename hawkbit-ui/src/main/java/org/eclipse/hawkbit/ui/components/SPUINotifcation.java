/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.server.FontAwesome;

/**
 * Common Util for Notifcation Message.
 * 
 *
 * 
 *
 *
 */
public final class SPUINotifcation {

    /**
     * private constructor.
     */
    private SPUINotifcation() {

    }

    /**
     * Display success type of notification message.
     * 
     * @param notificationMessage
     *            as reference
     * @param message
     *            is the message to displayed as success.
     */
    public static void displaySuccess(final SPNotificationMessage notificationMessage, final String message) {
        notificationMessage.showNotification(SPUILabelDefinitions.SP_NOTIFICATION_SUCCESS_MESSAGE_STYLE, null, message,
                true);
    }

    /**
     * Display error type of notification message.
     * 
     * @param notificationMessage
     *            as reference
     * @param message
     *            as message.
     */
    public static void displayValidationError(final SPNotificationMessage notificationMessage, final String message) {
        final StringBuilder updatedMsg = new StringBuilder(FontAwesome.EXCLAMATION_TRIANGLE.getHtml());
        updatedMsg.append(' ');
        updatedMsg.append(message);
        notificationMessage.showNotification(SPUILabelDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, null,
                updatedMsg.toString(), true);
    }

}
