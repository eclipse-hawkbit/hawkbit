package org.eclipse.hawkbit.ui.customrenderers.client;

import com.vaadin.client.connectors.AbstractRendererConnector;
import com.vaadin.shared.ui.Connect;

@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer.class)
public class HtmlLabelRendererConnector extends AbstractRendererConnector<String> {

    private static final long serialVersionUID = 7697966991925490786L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlLabelRenderer getRenderer() {
        return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlLabelRenderer) super.getRenderer();
    }

}
