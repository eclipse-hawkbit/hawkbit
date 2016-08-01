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

import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;

/**
 * Notification message component for displaying errors in the UI.
 */
public class HawkbitErrorNotificationMessage extends Notification {

    private static final long serialVersionUID = -6512576924243195753L;

    /**
     * Constructor of HawkbitErrorNotificationMessage
     * 
     * @param style
     *            style of the notification message
     * @param caption
     *            caption of the notification message
     * @param description
     *            text which is displayed in the notification
     * @param autoClose
     *            boolean if notification is closed after random click (true) or
     *            closed by clicking on the notification (false)
     */
    public HawkbitErrorNotificationMessage(final String style, final String caption, final String description,
            final boolean autoClose) {
        super(caption);
        setStyleName(style);
        if (autoClose) {
            setDelayMsec(SPUILabelDefinitions.SP_DELAY);
        } else {
            setDelayMsec(-1);
        }
        setHtmlContentAllowed(true);
        setPosition(Position.BOTTOM_RIGHT);
        setDescription(description);
    }

}
