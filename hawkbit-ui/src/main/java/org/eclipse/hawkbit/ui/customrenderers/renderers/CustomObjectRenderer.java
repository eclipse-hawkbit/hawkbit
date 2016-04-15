/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.customrenderers.renderers;

import org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObject;

import com.vaadin.ui.renderers.ClickableRenderer;

import elemental.json.JsonValue;

/**
 * Renders button with provided CustomObject.
 * Used to display button with link. 
 *
 */

public class CustomObjectRenderer extends ClickableRenderer<CustomObject> {

	private static final long serialVersionUID = -8754180585906263554L;

	/**
	 * Creates a new custom object renderer.
	 */
	public CustomObjectRenderer() {
		super(CustomObject.class, null);
	}
	
	/**
	 * Initialize custom object renderer with {@link Class<CustomObject>}
	 * 
	 * @param presentationType
	 *        Class<CustomObject>
	 */

	public CustomObjectRenderer(Class<CustomObject> presentationType) {
		super(presentationType);
	}

	/**
	 * Creates a new custom object renderer and adds the given click listener to it.
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
