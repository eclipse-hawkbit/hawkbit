/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.connectors.ClickableRendererConnector;
import com.vaadin.client.renderers.ClickableRenderer.RendererClickHandler;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

/**
 * A connector for {@link CustomObjectRenderer }.
 *
 */
@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.RolloutRenderer.class)
public class RolloutRendererConnector extends ClickableRendererConnector<RolloutRendererData> {
    private static final long serialVersionUID = 7734682321931830566L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRenderer) super.getRenderer();
    }

    @Override
    protected HandlerRegistration addClickHandler(final RendererClickHandler<JsonObject> handler) {
        return getRenderer().addClickHandler(handler);
    }
}
