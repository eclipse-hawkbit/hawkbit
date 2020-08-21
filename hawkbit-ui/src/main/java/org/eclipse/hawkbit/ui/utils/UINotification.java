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

import org.eclipse.hawkbit.ui.common.notification.ParallelNotification;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Show success, warning and error messages.
 */
@UIScope
@SpringComponent
public class UINotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Display success type of notification message.
     * 
     * @param message
     *            is the message to displayed as success.
     */
    public void displaySuccess(final String message) {
        showNotification(SPUIStyleDefinitions.SP_NOTIFICATION_SUCCESS_MESSAGE_STYLE, message, VaadinIcons.CHECK_CIRCLE);
    }

    /**
     * Display warning type of notification message.
     * 
     * @param message
     *            is the message to displayed as warning.
     */
    public void displayWarning(final String message) {
        showNotification(SPUIStyleDefinitions.SP_NOTIFICATION_WARNING_MESSAGE_STYLE, message, VaadinIcons.WARNING);
    }

    /**
     * Display error type of notification message.
     * 
     * @param message
     *            as message.
     */
    public void displayValidationError(final String message) {
        showNotification(SPUIStyleDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, message,
                VaadinIcons.EXCLAMATION_CIRCLE);
    }

    /**
     * Display generic notification.
     * 
     * @param styleName
     *            Style of the message
     * @param description
     *            Description of the message
     * @param icon
     *            Icon of the message
     */
    public static void showNotification(final String styleName, final String description, final Resource icon) {
        showNotification(styleName, null, description, icon, true);
    }

    /**
     * Display generic notification.
     * 
     * @param styleName
     *            Style of the message
     * @param caption
     *            Caption of the message
     * @param description
     *            Description of the message
     * @param icon
     *            Icon of the message
     * @param autoClose
     *            Should notification be automatically closed
     */
    public static void showNotification(final String styleName, final String caption, final String description,
            final Resource icon, final Boolean autoClose) {
        buildNotification(styleName, caption, description, icon, autoClose).show(Page.getCurrent());
    }

    /**
     * Builds UI notification.
     * 
     * @param styleName
     *            Style of the message
     * @param caption
     *            Caption of the message
     * @param description
     *            Description of the message
     * @param icon
     *            Icon of the message
     * @param autoClose
     *            Should notification be automatically closed
     * @return UI notification to be shown
     */
    public static ParallelNotification buildNotification(final String styleName, final String caption,
            final String description, final Resource icon, final Boolean autoClose) {
        final ParallelNotification notification = new ParallelNotification(caption, description);

        notification.setIcon(icon);
        notification.setStyleName(styleName);
        notification.setHtmlContentAllowed(false);
        notification.setPosition(Position.BOTTOM_RIGHT);

        if (autoClose) {
            notification.setDelayMsec(SPUILabelDefinitions.SP_DELAY);
        } else {
            notification.setDelayMsec(-1);
        }

        return notification;
    }
}
