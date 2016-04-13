package org.eclipse.hawkbit.ui.customrenderers.client;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObject;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.connectors.ClickableRendererConnector;
import com.vaadin.client.renderers.ClickableRenderer.RendererClickHandler;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

@Connect(org.eclipse.hawkbit.ui.customrenderers.renderers.CustomObjectRenderer.class)
public class CustomObjectRendererConnector extends ClickableRendererConnector<CustomObject> {
	private static final long serialVersionUID = 7734682321931830566L;

	public org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObjectRenederer getRenderer() {
		return (org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObjectRenederer) super.getRenderer();
	}

	@Override
	protected HandlerRegistration addClickHandler(
			RendererClickHandler<JsonObject> handler) {
        return getRenderer().addClickHandler(handler);
	}
}