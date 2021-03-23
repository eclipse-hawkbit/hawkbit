/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error;

public class UiErrorDetails {
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

    public boolean isPresent() {
        return caption != null && description != null;
    }

    public static UiErrorDetails empty() {
        return EMPTY;
    }

    public static UiErrorDetails create(final String caption, final String description) {
        return new UiErrorDetails(caption, description);
    }
}
