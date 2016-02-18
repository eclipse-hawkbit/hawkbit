package org.eclipse.hawkbit.ui.distributionbar.client;

import com.vaadin.client.connectors.ButtonRendererConnector;
import com.vaadin.shared.ui.Connect;

@Connect(org.eclipse.hawkbit.ui.distributionbar.renderers.LinkRenderer.class)
public class LinkRendererConnector extends ButtonRendererConnector {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.distributionbar.client.renderers.LinkRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.distributionbar.client.renderers.LinkRenderer) super.getRenderer();
    }
}