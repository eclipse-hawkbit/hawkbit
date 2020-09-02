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
 * Payload event to resize the layout
 */
public class LayoutResizeEventPayload extends EventLayoutViewAware {
    private final ResizeType resizeType;

    /**
     * Constructor for LayoutResizeEventPayload
     *
     * @param resizeType
     *          ResizeType
     * @param layout
     *          EventLayout
     * @param view
     *          EventView
     */
    public LayoutResizeEventPayload(final ResizeType resizeType, final EventLayout layout, final EventView view) {
        super(layout, view);

        this.resizeType = resizeType;
    }

    /**
     * @return Layout resize type
     */
    public ResizeType getResizeType() {
        return resizeType;
    }

    /**
     * Constants for Layout resize type
     */
    public enum ResizeType {
        MINIMIZE, MAXIMIZE;
    }
}
