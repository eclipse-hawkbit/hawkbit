/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Style for button: Small NoBorder Height and size UnDefined.
 * 
 *
 * 
 *
 * 
 *
 */
public class SPUIButtonStyleSmallNoBorderUHS implements SPUIButtonDecorator {

    /**
     * Decorate Button and return.
     * 
     * @param button
     *            as Button
     * @param style
     *            as String
     * @param setStyle
     *            as String
     * @param icon
     *            as resource
     * @return Button
     */
    public Button decorate(Button button, String style, boolean setStyle, Resource icon) {
        button.addStyleName(ValoTheme.BUTTON_SMALL);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        button.setHeightUndefined();
        button.setSizeUndefined();
        // Set Style
        if (null != style) {
            if (setStyle) {
                button.setStyleName(style);
            } else {
                button.addStyleName(style);
            }
        }
        // Set icon
        if (null != icon) {
            button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
            button.setIcon(icon);
        }
        return button;
    }

}
