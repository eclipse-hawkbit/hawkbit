/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.widget.grid.RendererCellReference;

public class HtmlButtonRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        if (text != null) {
            button.setHTML(text);
        }
        applystyles(button);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId("rollout.action.button.id");
    }

    private void applystyles(Button button) {
        button.setStyleName(VButton.CLASSNAME);
        button.addStyleName(getStyle("tiny"));
        button.addStyleName(getStyle("borderless"));
        button.addStyleName(getStyle("icon-only"));
        button.addStyleName(getStyle("button-no-border"));
    }
    
    
    private String getStyle(final String style) {
        return new StringBuilder(style).append(" ").append(VButton.CLASSNAME).append("-").append(style).toString();
    }
}
