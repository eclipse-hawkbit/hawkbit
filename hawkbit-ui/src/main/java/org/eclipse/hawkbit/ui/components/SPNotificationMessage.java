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

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;

/**
 * Notification message component.
 *
 *
 *
 */
public class SPNotificationMessage extends Notification {

    /**
     * ID.
     */
    private static final long serialVersionUID = -6512576924243195753L;

    /**
     * Constructor.
     */
    public SPNotificationMessage() {
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
     * @param page
     *            current {@link Page}
     */
    public void showNotification(final String styleName, final String caption, final String description,
            final Boolean autoClose, final Page page) {
        decorate(styleName, caption, description, autoClose);
        this.show(page);
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
