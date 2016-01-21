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
 *
 * Large icon button created to show or hide button.
 * 
 *
 *
 *
 *
 */
public class SPUIButtonStyleLargeIconNoBorder implements SPUIButtonDecorator {

    /**
     * {@inheritDoc}
     */
    @Override
    public Button decorate(Button button, String style, boolean setStyle, Resource icon) {
        button.addStyleName(ValoTheme.BUTTON_LARGE);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
        if (setStyle && style != null) {
            button.addStyleName(style);
        }
        button.setWidthUndefined();
        // Set icon
        if (null != icon) {
            button.setIcon(icon);
        }
        return button;

    }

}
