/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import com.google.gwt.core.shared.GWT;
import com.vaadin.client.renderers.ClickableRenderer;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.widget.grid.RendererCellReference;

/**
 * Renders button with provided CustomObject.
 * Used to display button with link.
 *
 */
public class RolloutRenderer extends ClickableRenderer<RolloutRendererData, VButton> {

	@Override
	public VButton createWidget() {
		VButton b = GWT.create(VButton.class);
		b.addClickHandler(this);
		b.setStylePrimaryName("v-nativebutton");
		return b;
	}

	@Override
	public void render(RendererCellReference cell, RolloutRendererData text, VButton button) {
		final String creating = "CREATING";
		button.setText(text.getName());
		applystyle(button);
		// this is to allow the button to disappear, if the text is null
		button.setVisible(text.getName() != null);
		button.getElement().setId(new StringBuilder("link").append(".").append(text.getName()).toString());
		/*
		 * checking Rollout Status for applying button style. If Rollout status
		 * is not "CREATING", then the Rollout button is applying hyperlink
		 * style
		 */
		final boolean isStatusCreate = text.getStatus() != null && creating.equalsIgnoreCase(text.getStatus());
		if (isStatusCreate) {
			button.addStyleName(getStyle("boldhide"));
			button.setEnabled(false);
		} else {
			button.setEnabled(true);
		}
	}

	private void applystyle(VButton button) {
		button.setStyleName(VButton.CLASSNAME);
		button.addStyleName(getStyle("borderless"));
		button.addStyleName(getStyle("small"));
		button.addStyleName(getStyle("on-focus-no-border"));
		button.addStyleName(getStyle("link"));
	}

	private String getStyle(final String style) {
		return new StringBuilder(style).append(" ").append(VButton.CLASSNAME).append("-").append(style).toString();
	}

}