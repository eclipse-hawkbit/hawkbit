/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error;

/**
 * Details of UI errors for building the error notification.
 */
public final class UiErrorDetails {
    private static final UiErrorDetails EMPTY = new UiErrorDetails();

    private final String caption;
    private final String description;

    private UiErrorDetails() {
        this(null, null);
    }

    private UiErrorDetails(final String caption, final String description) {
        this.caption = caption;
        this.description = description;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if error details are not empty.
     * 
     * @return if error details are populated
     */
    public boolean isPresent() {
        return caption != null && description != null;
    }

    /**
     * Creates empty error details that should be ignored by error handler.
     * 
     * @return empty error details
     */
    public static UiErrorDetails empty() {
        return EMPTY;
    }

    /**
     * Creates error details that should be processed by error handler.
     * 
     * @param caption
     *            error details caption
     * @param description
     *            error details description
     * @return error details
     */
    public static UiErrorDetails create(final String caption, final String description) {
        return new UiErrorDetails(caption, description);
    }
}
