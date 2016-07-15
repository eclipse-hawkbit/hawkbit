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
 * Decorator class for a borderless Button with icon.
 */
public class SPUIButtonStyleNoBorderWithIcon implements SPUIButtonDecorator {

    private Button button;

    @Override
    public Button decorate(final Button button, final String style, final boolean setStyle, final Resource icon) {

        this.button = button;

        button.addStyleName(ValoTheme.LABEL_SMALL);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);

        setButtonStyle(style, setStyle);
        setButtonIcon(icon);

        button.setSizeFull();

        setButtonStyle(style, setStyle);

        return button;
    }

    /**
     * It is possible to add or set a new style to the button. If a new style is
     * set the other styles (LABEL_SMALL and BUTTON_BORDERLESS_COLORED) will be
     * overwritten
     * 
     * @param style
     *            styleName
     * @param setStyle
     *            boolean: Trigger if the style should be added or replace the
     *            other styles
     */
    private void setButtonStyle(final String style, final boolean setStyle) {

        if (StringUtils.isEmpty(style)) {
            return;
        }

        if (setStyle) {
            button.setStyleName(style);
        } else {
            button.addStyleName(style);
        }
    }

    private void setButtonIcon(final Resource icon) {

        if (icon == null) {
            return;
        }
        button.setIcon(icon);
    }
}
