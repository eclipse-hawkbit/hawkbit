/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.widget.grid.RendererCellReference;

/**
 * Renders button with provided HTML content. Used to display button with icons.
 */
public class HtmlButtonRenderer extends ButtonRenderer {

    public static final String DISABLE_VALUE = "_Disabled_";

    @Override
    public void render(final RendererCellReference cell, final String text, final Button button) {
        final boolean buttonEnable = isButtonEnable(cell.getElement().getClassName());
        if (text != null) {
            button.setHTML(text);
        }
        applystyles(button, buttonEnable);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId(UIComponentIdProvider.ROLLOUT_ACTION_ID + "." + cell.getColumnIndex());
        button.setEnabled(buttonEnable);
    }

    /**
     * see here https://vaadin.com/forum#!/thread/9418390/9765924
     * 
     * @param text
     *            the button text
     * @return is button enable.
     */
    private static boolean isButtonEnable(final String text) {
        return !text.contains(DISABLE_VALUE);
    }

    private void applystyles(final Button button, final boolean buttonEnable) {

        button.setStyleName(VButton.CLASSNAME);
        button.addStyleName(getStyle("tiny"));
        button.addStyleName(getStyle("borderless-colored"));
        button.addStyleName(getStyle("button-no-border"));
        button.addStyleName(getStyle("action-type-padding"));

        if (buttonEnable) {
            return;
        }
        button.addStyleName("v-disabled");
    }

    private String getStyle(final String style) {
        return new StringBuilder(style).append(" ").append(VButton.CLASSNAME).append("-").append(style).toString();
    }

}
