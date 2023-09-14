/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.notification;

import com.vaadin.ui.Notification;

/**
 * Creates {@link Notification} that do not hide each other
 *
 */
public class ParallelNotification extends Notification {
    private static final long serialVersionUID = 1L;

    /**
     * Creates notification
     *
     * @param caption
     *            The message to show
     */
    public ParallelNotification(final String caption) {
        super(caption);
    }

    /**
     * Creates notification
     *
     * @param caption
     *            The message to show
     * @param type
     *            The type of message
     */
    public ParallelNotification(final String caption, final Type type) {
        super(caption, type);
    }

    /**
     * Creates notification
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     */
    public ParallelNotification(final String caption, final String description) {
        super(caption, description);
    }

    /**
     * Creates notification
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     * @param type
     *            The type of message
     */
    public ParallelNotification(final String caption, final String description, final Type type) {
        super(caption, description, type);
    }

    /**
     * Creates notification
     *
     * @param caption
     *            The message caption
     * @param description
     *            The message description
     * @param type
     *            The type of message
     * @param htmlContentAllowed
     *            Whether html in the caption and description should be
     *            displayed as html or as plain text
     */
    public ParallelNotification(final String caption, final String description, final Type type,
            final boolean htmlContentAllowed) {
        super(caption, description, type, htmlContentAllowed);
    }

}