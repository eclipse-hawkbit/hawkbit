/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Style for button: Small NoBorder.
 */
public class SPUIButtonStyleNoBorder implements SPUIButtonDecorator {

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
    @Override
    public Button decorate(final Button button, final String style, final boolean setStyle, final Resource icon) {
        if (style != null && setStyle) {
            button.addStyleName(style);
        }
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        if (icon != null) {
            button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
            button.addStyleName("button-no-border");
            button.setIcon(icon);
        }

        return button;
    }

}
