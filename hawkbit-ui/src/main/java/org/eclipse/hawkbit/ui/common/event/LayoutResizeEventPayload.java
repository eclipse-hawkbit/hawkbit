/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
