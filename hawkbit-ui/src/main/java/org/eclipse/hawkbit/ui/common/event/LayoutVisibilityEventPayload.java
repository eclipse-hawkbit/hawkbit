/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Payload event for layout visibility
 */
public class LayoutVisibilityEventPayload extends EventLayoutViewAware {
    private final VisibilityType visibilityType;

    /**
     * Constructor for LayoutVisibilityEventPayload
     *
     * @param visibilityType
     *          VisibilityType
     * @param layout
     *          EventLayout
     * @param view
     *          EventView
     */
    public LayoutVisibilityEventPayload(final VisibilityType visibilityType, final EventLayout layout, final EventView view) {
        super(layout, view);

        this.visibilityType = visibilityType;
    }

    /**
     * @return Layout visibility type
     */
    public VisibilityType getVisibilityType() {
        return visibilityType;
    }

    /**
     * Constants for Layout visibility type
     */
    public enum VisibilityType {
        SHOW, HIDE;
    }
}
