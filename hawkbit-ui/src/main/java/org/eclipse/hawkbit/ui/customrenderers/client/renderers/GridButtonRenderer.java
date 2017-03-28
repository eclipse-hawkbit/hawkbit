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
import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ClickableRenderer;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.widget.grid.RendererCellReference;

/**
 * Renders button with provided HTML content. Used to display button with icons.
 */
public class GridButtonRenderer extends ClickableRenderer<FontIconData, Button> {

    @Override
    public Button createWidget() {
        Button b = GWT.create(Button.class);
        b.addClickHandler(this);
        b.setStylePrimaryName("v-nativebutton");
        return b;
    }

    @Override
    public void render(final RendererCellReference cell, final FontIconData iconMetadata, final Button button) {
        if (iconMetadata.getFontIconHtml() != null) {
            button.setHTML(iconMetadata.getFontIconHtml());
        }
        applyStyles(button, iconMetadata.isDisabled(), iconMetadata.getStyle());
        button.getElement().setId(iconMetadata.getId());
        button.getElement().setTitle(iconMetadata.getTitle());
        button.setEnabled(!iconMetadata.isDisabled());
        // this is to allow the button to disappear, if the text is null
        button.setVisible(iconMetadata.getFontIconHtml() != null);
    }

    private static void applyStyles(final Button button, final boolean buttonDisabled, final String additionalStyle) {

        button.setStyleName(VButton.CLASSNAME);
        button.addStyleName(getStyle("tiny"));
        button.addStyleName(getStyle("borderless"));
        button.addStyleName(getStyle("button-no-border"));
        button.addStyleName(getStyle("action-type-padding"));
        button.addStyleName(getStyle(additionalStyle));

        if (buttonDisabled) {
            button.addStyleName("v-disabled");
        }
    }

    private static String getStyle(final String style) {
        return new StringBuilder(style).append(" ").append(VButton.CLASSNAME).append("-").append(style).toString();
    }

}
