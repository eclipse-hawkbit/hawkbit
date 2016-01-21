/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

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
     * @param promt
     *            as promt
     * @return ComboBox as comp
     */
    public static ComboBox decorate(final String height, final String width, final String style, final String styleName,
            final boolean required, final String data, final String promt) {
        final ComboBox spUICombo = new ComboBox();
        // Default settings
        spUICombo.setRequired(required);
        spUICombo.addStyleName(ValoTheme.COMBOBOX_TINY);

        // Add style
        if (null != style) {
            spUICombo.setStyleName(style);
        }
        // Add style Name
        if (null != styleName) {
            spUICombo.addStyleName(styleName);
        }
        // Add height
        if (null != height) {
            spUICombo.setHeight(height);
        }
        // AddWidth
        if (null != width) {
            spUICombo.setWidth(width);
        }
        // Set promt
        if (null != promt) {
            spUICombo.setInputPrompt(promt);
        }
        // Set Data
        if (null != data) {
            spUICombo.setData(data);
        }

        return spUICombo;
    }

}
