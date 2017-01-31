/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Show success and error messages.
 */
@UIScope
@SpringComponent
public class UINotification implements Serializable {

    private static final long serialVersionUID = -9030485417988977466L;

    private final NotificationMessage notificationMessage;

    @Autowired
    UINotification(final NotificationMessage notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    /**
     * Display success type of notification message.
     * 
     * @param message
     *            is the message to displayed as success.
     */
    public void displaySuccess(final String message) {
        notificationMessage.showNotification(SPUILabelDefinitions.SP_NOTIFICATION_SUCCESS_MESSAGE_STYLE, null, message,
                true);
    }

    /**
     * Display warning type of notification message.
     * 
     * @param message
     *            is the message to displayed as warning.
     */
    public void displayWarning(final String message) {
        notificationMessage.showNotification(SPUILabelDefinitions.SP_NOTIFICATION_WARNING_MESSAGE_STYLE, null, message,
                true);
    }

    /**
     * Display error type of notification message.
     * 
     * @param message
     *            as message.
     */
    public void displayValidationError(final String message) {
        final StringBuilder updatedMsg = new StringBuilder(FontAwesome.EXCLAMATION_TRIANGLE.getHtml());
        updatedMsg.append(' ');
        updatedMsg.append(message);
        notificationMessage.showNotification(SPUILabelDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, null,
                updatedMsg.toString(), true);
    }

}
