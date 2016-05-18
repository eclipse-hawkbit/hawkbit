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

public class SPUIButtonStyleBorderWithIcon implements SPUIButtonDecorator {

    /**
     * Style for button: Primary.
     */

    @Override
    public Button decorate(final Button button, final String style, final boolean setStyle, final Resource icon) {

        // Set Style
        if (null != style) {
            if (setStyle) {
                button.setStyleName(style);
            } else {
                button.addStyleName(style);
            }
        }
        button.addStyleName(ValoTheme.LABEL_SMALL);
        button.setSizeFull();

        // Set icon
        if (null != icon) {
            button.setIcon(icon);
        }
        return button;
    }

}
