/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

/**
 * TextArea with required style.
 * 
 *
 *
 */
public final class SPUITextAreaDecorator {

    /**
     * Private Constrcutor.
     */
    private SPUITextAreaDecorator() {

    }

    /**
     * Decorate.
     * 
     * @param style
     *            set style
     * @param styleName
     *            add style
     * @param required
     *            to set field as mandatory
     * @param data
     *            component data
     * @param promt
     *            as user for input
     * @param maxLength
     *            maximum characters allowed
     * @return TextArea as comp
     */
    public static TextArea decorate(String style, String styleName, boolean required, String data, String promt,
            int maxLength) {
        final TextArea spUITxtArea = new TextArea();
        // Default settings
        spUITxtArea.setRequired(false);
        spUITxtArea.addStyleName(ValoTheme.COMBOBOX_SMALL);
        if (required) {
            spUITxtArea.setRequired(true);
        }
        // Add style
        if (null != style) {
            spUITxtArea.setStyleName(style);
        }
        // Add style Name
        if (null != styleName) {
            spUITxtArea.addStyleName(styleName);
        }
        if (null != promt) {
            spUITxtArea.setInputPrompt(promt);
        }
        if (null != data) {
            spUITxtArea.setData(data);
        }

        if (maxLength > 0) {
            spUITxtArea.setMaxLength(maxLength);
        }
        return spUITxtArea;
    }

}
