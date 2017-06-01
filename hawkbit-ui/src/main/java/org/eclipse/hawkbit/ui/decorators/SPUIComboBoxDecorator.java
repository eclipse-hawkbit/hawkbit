/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import org.springframework.util.StringUtils;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

/**
 * ComboBox with required style.
 * 
 *
 *
 */
public final class SPUIComboBoxDecorator {

    /**
     * Private Constructor.
     */
    private SPUIComboBoxDecorator() {

    }

    /**
     * Decorate.
     * 
     * @param caption
     *            caption of the combobox
     * @param height
     *            as H
     * @param width
     *            as W
     * @param style
     *            as style
     * @param styleName
     *            as style name
     * @param required
     *            as T|F
     * @param data
     *            as data
     * @param prompt
     *            as promt
     * @return ComboBox as comp
     */
    public static ComboBox decorate(final String caption, final String width, final String style,
            final String styleName, final boolean required, final String data, final String prompt) {
        final ComboBox spUICombo = new ComboBox();
        // Default settings
        spUICombo.setRequired(required);
        spUICombo.addStyleName(ValoTheme.COMBOBOX_TINY);

        if (!StringUtils.isEmpty(caption)) {
            spUICombo.setCaption(caption);
        }
        // Add style
        if (!StringUtils.isEmpty(style)) {
            spUICombo.setStyleName(style);
        }
        // Add style Name
        if (!StringUtils.isEmpty(styleName)) {
            spUICombo.addStyleName(styleName);
        }
        // AddWidth
        if (!StringUtils.isEmpty(width)) {
            spUICombo.setWidth(width);
        }
        // Set prompt
        if (!StringUtils.isEmpty(prompt)) {
            spUICombo.setInputPrompt(prompt);
        }
        // Set Data
        if (!StringUtils.isEmpty(data)) {
            spUICombo.setData(data);
        }

        return spUICombo;
    }

}
