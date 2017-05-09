/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.renderers;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.FontIconData;

import com.vaadin.ui.renderers.ClickableRenderer;

import elemental.json.JsonValue;

/**
 * Renders buttons for a grid with provided HTML content based on meta-data.
 * Used to display button with icons.
 */
public class GridButtonRenderer extends ClickableRenderer<FontIconData> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public GridButtonRenderer() {
        super(FontIconData.class, null);
    }

    /**
     * Creates a new custom object renderer and adds the given click listener to
     * it.
     *
     * @param listener
     *            the click listener to register
     */
    public GridButtonRenderer(final RendererClickListener listener) {
        this();
        addClickListener(listener);
    }



    /**
     * Initialize custom object renderer with the given type.
     *
     * @param presentationType
     *            Class<CustomObject>
     */

    public GridButtonRenderer(final Class<FontIconData> presentationType) {
        super(presentationType);
    }


    @Override
    public JsonValue encode(final FontIconData resource) {
        return super.encode(resource, FontIconData.class);
    }
}
