/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.FontIconData;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.connectors.ClickableRendererConnector;
import com.vaadin.client.renderers.ClickableRenderer.RendererClickHandler;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

/**
 * A connector for {@link GridButtonRenderer }.
 *
 */
@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.GridButtonRenderer.class)
public class GridButtonRendererConnector extends ClickableRendererConnector<FontIconData> {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.GridButtonRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.GridButtonRenderer) super.getRenderer();
    }

    @Override
    protected HandlerRegistration addClickHandler(final RendererClickHandler<JsonObject> handler) {
        return getRenderer().addClickHandler(handler);
    }
}
