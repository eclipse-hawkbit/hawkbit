/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.themes.ValoTheme;

/**
 * ComboBox with required style.
 */
public class SPUICheckBox extends CheckBox {

    private static final long serialVersionUID = 2270323611612189225L;

    SPUICheckBox(final String caption, final String style, final String styleName, final boolean required,
            final String data) {
        decorate(caption, style, styleName, required, data);
    }

    /**
     * decorate.
     * 
     * @param style
     *            style of the CheckBox
     * @param styleName
     *            styleName of the CheckBox
     * @param required
     *            required check for Checkbox
     * @param data
     *            data of the CheckBox
     * @param promt
     *            inputpromt of the CheckBox
     */
    private void decorate(final String caption, final String style, final String styleName, final boolean required,
            final String data) {
        // Default settings
        setRequired(required);
        addStyleName(ValoTheme.CHECKBOX_SMALL);
        if (null != caption) {
            setCaption(caption);
        }
        // Add style
        if (null != style) {
            setStyleName(style);
        }
        // Add style Name
        if (null != styleName) {
            addStyleName(styleName);
        }
        // Set Data
        if (null != data) {
            setData(data);
        }
    }

}
