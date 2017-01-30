/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Notification;

/**
 * Show notification messages.
 */
@UIScope
@SpringComponent
public class NotificationMessage extends Notification {

    private static final long serialVersionUID = -7355179454812323243L;

    /**
     * Default constructor of notification message.
     */
    public NotificationMessage() {
        super("");
    }

    /**
     * Notification message component.
     * 
     * @param styleName
     *            style name of message
     * @param caption
     *            message caption
     * @param description
     *            message description
     * @param autoClose
     *            flag to indicate enable close option
     */
    void showNotification(final String styleName, final String caption, final String description,
            final Boolean autoClose) {
        decorate(styleName, caption, description, autoClose);
        this.show(Page.getCurrent());
    }

    /**
     * Decorate.
     * 
     * @param styleName
     *            style name of message
     * @param caption
     *            message caption
     * @param description
     *            message description
     * @param autoClose
     *            flag to indicate enable close option
     */
    private void decorate(final String styleName, final String caption, final String description,
            final Boolean autoClose) {
        setCaption(caption);
        setDescription(description);
        setStyleName(styleName);
        setHtmlContentAllowed(true);
        setPosition(Position.BOTTOM_RIGHT);
        if (autoClose) {
            setDelayMsec(SPUILabelDefinitions.SP_DELAY);
        } else {
            setDelayMsec(-1);
        }
    }
}
