/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.renderers.LinkRenderer;

import com.vaadin.client.connectors.ButtonRendererConnector;
import com.vaadin.shared.ui.Connect;

/**
 * 
 * A connector for {@link LinkRenderer}.
 *
 */
@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.LinkRenderer.class)
public class LinkRendererConnector extends ButtonRendererConnector {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.LinkRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.LinkRenderer) super.getRenderer();
    }
}