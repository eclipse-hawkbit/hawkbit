package org.eclipse.hawkbit.ui.distributionbar.client;

import com.vaadin.client.connectors.ButtonRendererConnector;
import com.vaadin.shared.ui.Connect;

@Connect(org.eclipse.hawkbit.ui.distributionbar.renderers.HtmlButtonRenderer.class)
public class HtmlButtonRendererConnector extends ButtonRendererConnector {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.distributionbar.client.renderers.HtmlButtonRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.distributionbar.client.renderers.HtmlButtonRenderer) super.getRenderer();
    }
}