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
public class HawkbitErrorNotificationMessage extends Notification {

    /**
     * ID.
     */
    private static final long serialVersionUID = -6512576924243195753L;

    /**
     * Constructor.
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
