/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Decorate TextField with required style.
 * 
 *
 *
 */
public final class SPUITextFieldDecorator {

    /**
     * Constructor.
     */
    private SPUITextFieldDecorator() {

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
     *            prompt user for input
     * @param immediate
     *            as for display
     * @param maxLengthAllowed
     *            maximum characters allowed
     * @return Text field as decorated
     */
    public static TextField decorate(String style, String styleName, boolean required, String data, String promt,
            boolean immediate, int maxLengthAllowed) {
        final TextField spUITxtFld = new TextField();
        // Default settings
        spUITxtFld.setRequired(false);
        spUITxtFld.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        if (required) {
            spUITxtFld.setRequired(true);
        }
        if (immediate) {
            spUITxtFld.setImmediate(true);
        }
        // Add style
        if (null != style) {
            spUITxtFld.setStyleName(style);
        }
        // Add style Name
        if (null != styleName) {
            spUITxtFld.addStyleName(styleName);
        }
        if (null != promt) {
            spUITxtFld.setInputPrompt(promt);
        }
        if (null != data) {
            spUITxtFld.setData(data);
        }
        if (maxLengthAllowed > 0) {
            spUITxtFld.setMaxLength(maxLengthAllowed);
        }

        return spUITxtFld;
    }

}
