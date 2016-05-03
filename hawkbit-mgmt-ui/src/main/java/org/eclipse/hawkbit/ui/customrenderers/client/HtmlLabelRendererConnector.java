/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;

import com.vaadin.client.connectors.AbstractRendererConnector;
import com.vaadin.shared.ui.Connect;

/**
 * 
 * A connector for {@link HtmlLabelRenderer}.
 *
 */
@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer.class)
public class HtmlLabelRendererConnector extends AbstractRendererConnector<String> {

    private static final long serialVersionUID = 7697966991925490786L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlLabelRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlLabelRenderer) super.getRenderer();
    }

}
