/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.customrenderers.renderers;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;

import com.vaadin.ui.renderers.ClickableRenderer;

import elemental.json.JsonValue;

/**
 * Renders button with provided CustomObject. Used to display button with link.
 */
public class RolloutRenderer extends ClickableRenderer<RolloutRendererData> {

    private static final long serialVersionUID = -8754180585906263554L;

    /**
     * Creates a new custom object renderer.
     */
    public RolloutRenderer() {
        super(RolloutRendererData.class, null);
    }

    /**
     * Initialize custom object renderer with the given type.
     *
     * @param presentationType
     *            Class<CustomObject>
     */

    public RolloutRenderer(final Class<RolloutRendererData> presentationType) {
        super(presentationType);
    }

    /**
     * Creates a new custom object renderer and adds the given click listener to
     * it.
     *
     * @param listener
     *            the click listener to register
     */
    public RolloutRenderer(final RendererClickListener listener) {
        this();
        addClickListener(listener);
    }

    @Override
    public JsonValue encode(final RolloutRendererData resource) {
        return super.encode(resource, RolloutRendererData.class);
    }
}
