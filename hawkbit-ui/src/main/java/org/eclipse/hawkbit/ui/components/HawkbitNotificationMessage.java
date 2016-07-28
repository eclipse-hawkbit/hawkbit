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
 * Notification message component.
 */
public class HawkbitNotificationMessage extends Notification {

    /**
     * ID.
     */
    private static final long serialVersionUID = -6512576924243195753L;

    /**
     * Constructor.
     */
    public HawkbitNotificationMessage() {
        super("");
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
    public void decorateWith(final String styleName, final String caption, final String description,
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
