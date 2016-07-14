/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Button with icon decorator.
 *
 */
public class SPUIButtonStyleBorderWithIcon implements SPUIButtonDecorator {

    private Button button;

    @Override
    public Button decorate(final Button button, final String style, final boolean setStyle, final Resource icon) {

        this.button = button;

        setButtonStyle(style, setStyle);
        setButtonIcon(icon);

        button.addStyleName(ValoTheme.LABEL_SMALL);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        button.setSizeFull();

        return button;
    }

    private void setButtonStyle(final String style, final boolean setStyle) {

        if (StringUtils.isEmpty(style)) {
            return;
        }

        if (setStyle) {
            button.setStyleName(style);
        }
    }

    private void setButtonIcon(final Resource icon) {

        if (icon == null) {
            return;
        }
        button.setIcon(icon);
    }
}
