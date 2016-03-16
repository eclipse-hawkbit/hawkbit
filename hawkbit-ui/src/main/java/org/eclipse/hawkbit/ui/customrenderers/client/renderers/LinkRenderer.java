/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.ui.VButton;
import com.vaadin.client.widget.grid.RendererCellReference;

/**
 * 
 * Renders link with provided text.
 *
 */
public class LinkRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        Map<String, String> nameStatusMap = formatData(text);
        final String targetName = nameStatusMap.get("name");
        final String targetStatus = nameStatusMap.get("status");
        button.setText(targetName);
        applystyle(button);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(targetName != null);
        button.getElement().setId(new StringBuilder("link").append(".").append(targetName).toString());
        /*
         * checking target Status for applying button style. If target status is
         * not "CREATING", then the Rollout button is applying hyperlink style
         */
        final boolean isStatusCreate = targetStatus != null && "CREATING".equalsIgnoreCase(targetStatus);
        button.getElement().setAttribute("enabled", isStatusCreate ? "false" : "true");
        button.addStyleName(isStatusCreate ? getStyle("link") : "");
    }

    private void applystyle(Button button) {
        button.setStyleName(VButton.CLASSNAME);
        button.addStyleName(getStyle("borderless"));
        button.addStyleName(getStyle("small"));
        button.addStyleName(getStyle("on-focus-no-border"));
    }

    private String getStyle(final String style) {
        return new StringBuilder(style).append(" ").append(VButton.CLASSNAME).append("-").append(style).toString();
    }

    private Map<String, String> formatData(String input) {
        Map<String, String> details = new HashMap<>();
        String[] tempData = input.split(",");
        for (String targetData : tempData) {
            String[] targetDataList = targetData.split(":");
            details.put(targetDataList[0], targetDataList[1]);
        }
        return details;
    }
}
