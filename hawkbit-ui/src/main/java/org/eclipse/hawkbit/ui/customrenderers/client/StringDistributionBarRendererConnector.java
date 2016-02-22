package org.eclipse.hawkbit.ui.customrenderers.client;

import com.vaadin.client.connectors.AbstractRendererConnector;
import com.vaadin.shared.ui.Connect;

@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.StringDistributionBarRenderer.class)
public class StringDistributionBarRendererConnector extends AbstractRendererConnector<String> {

    private static final long serialVersionUID = 7697966991925490786L;

    @Override
    public org.eclipse.hawkbit.ui.customrenderers.client.renderers.StringDistributionBarRenderer getRenderer() {
        org.eclipse.hawkbit.ui.customrenderers.client.renderers.StringDistributionBarRenderer renderer = (org.eclipse.hawkbit.ui.customrenderers.client.renderers.StringDistributionBarRenderer) super.getRenderer();
        renderer.setUiWidgetClassName(StringDistributionBarRendererConnector.this.getConnection().getUIConnector().getWidget().getParent().getStyleName());
        return renderer;
    }

}
