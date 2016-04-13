package org.eclipse.hawkbit.ui.customrenderers.renderers;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObject;

import com.vaadin.ui.renderers.ClickableRenderer;

import elemental.json.JsonValue;

public class CustomObjectRenderer extends ClickableRenderer<CustomObject> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8754180585906263554L;

	/**
	 * Creates a new image renderer.
	 */
	public CustomObjectRenderer() {
		super(CustomObject.class, null);
	}

	public CustomObjectRenderer(Class<CustomObject> presentationType) {
		super(presentationType);
	}

	/**
	 * Creates a new image renderer and adds the given click listener to it.
	 * 
	 * @param listener
	 *            the click listener to register
	 */
	public CustomObjectRenderer(RendererClickListener listener) {
		this();
		addClickListener(listener);
	}

	@Override
	public JsonValue encode(CustomObject resource) {
		return super.encode(resource, CustomObject.class);
	}
}
