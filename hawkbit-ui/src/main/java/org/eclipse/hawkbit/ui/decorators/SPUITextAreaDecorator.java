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
     * @param prompt
     *            as user for input
     * @param maxLength
     *            maximum characters allowed
     * @return TextArea as comp
     */
    public static TextArea decorate(final String caption, final String style, final String styleName,
            final boolean required, final String data, final String prompt, final int maxLength) {
        final TextArea spUITxtArea = new TextArea();
        // Default settings
        spUITxtArea.setRequired(false);
        spUITxtArea.addStyleName(ValoTheme.TEXTAREA_SMALL);
        if (StringUtils.isNotEmpty(caption)) {
            spUITxtArea.setCaption(caption);
        }
        if (required) {
            spUITxtArea.setRequired(true);
        }
        // Add style
        if (StringUtils.isNotEmpty(style)) {
            spUITxtArea.setStyleName(style);
        }
        // Add style Name
        if (StringUtils.isNotEmpty(styleName)) {
            spUITxtArea.addStyleName(styleName);
        }
        if (StringUtils.isNotEmpty(prompt)) {
            spUITxtArea.setInputPrompt(prompt);
        }
        if (StringUtils.isNotEmpty(data)) {
            spUITxtArea.setData(data);
        }

        if (maxLength > 0) {
            spUITxtArea.setMaxLength(maxLength);
        }
        return spUITxtArea;
    }

}
