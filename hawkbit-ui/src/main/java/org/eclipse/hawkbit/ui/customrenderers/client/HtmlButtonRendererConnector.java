/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;

import com.vaadin.client.connectors.ButtonRendererConnector;
import com.vaadin.shared.ui.Connect;

/**
 * A connector for {@link HtmlButtonRenderer}.
 *
 */
@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer.class)
public class HtmlButtonRendererConnector extends ButtonRendererConnector {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlButtonRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlButtonRenderer) super.getRenderer();
    }
}
