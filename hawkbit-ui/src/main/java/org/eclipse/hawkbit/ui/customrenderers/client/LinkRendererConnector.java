package org.eclipse.hawkbit.ui.customrenderers.client;

import com.vaadin.client.connectors.ButtonRendererConnector;
import com.vaadin.shared.ui.Connect;

@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.LinkRenderer.class)
public class LinkRendererConnector extends ButtonRendererConnector {
    private static final long serialVersionUID = 7987417436367399331L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.LinkRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.LinkRenderer) super.getRenderer();
    }
}