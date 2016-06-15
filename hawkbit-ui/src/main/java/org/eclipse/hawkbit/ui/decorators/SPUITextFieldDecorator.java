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
     * 
     * @param caption
     *            set the caption of the textfield
     * @param style
     *            set style
     * @param styleName
     *            add style
     * @param required
     *            to set field as mandatory
     * @param data
     *            component data
     * @param prompt
     *            prompt user for input
     * @param immediate
     *            as for display
     * @param maxLengthAllowed
     *            maximum characters allowed
     * @return Text field as decorated
     */
    public static TextField decorate(final String caption, final String style, final String styleName,
            final boolean required, final String data, final String prompt, final boolean immediate,
            final int maxLengthAllowed) {
        final TextField spUITxtFld = new TextField();
        if (StringUtils.isNotEmpty(caption)) {
            spUITxtFld.setCaption(caption);
        }
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
        if (StringUtils.isNotEmpty(style)) {
            spUITxtFld.setStyleName(style);
        }
        // Add style Name
        if (StringUtils.isNotEmpty(styleName)) {
            spUITxtFld.addStyleName(styleName);
        }
        if (StringUtils.isNotEmpty(prompt)) {
            spUITxtFld.setInputPrompt(prompt);
        }
        if (StringUtils.isNotEmpty(data)) {
            spUITxtFld.setData(data);
        }
        if (maxLengthAllowed > 0) {
            spUITxtFld.setMaxLength(maxLengthAllowed);
        }

        return spUITxtFld;
    }

}
