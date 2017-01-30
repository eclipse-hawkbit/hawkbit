/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import com.vaadin.ui.TextArea;
import com.vaadin.ui.themes.ValoTheme;

/**
 * TextArea builder.
 *
 */
public class TextAreaBuilder extends AbstractTextFieldBuilder<TextArea> {

    private static final int TEXT_AREA_DEFAULT_MAX_LENGTH = 512;

    /**
     * Constructor.
     */
    public TextAreaBuilder() {
        maxLengthAllowed(TEXT_AREA_DEFAULT_MAX_LENGTH);
        styleName(ValoTheme.TEXTAREA_SMALL);
    }

    @Override
    protected TextArea createTextComponent() {
        final TextArea textArea = new TextArea();
        textArea.addStyleName(ValoTheme.TEXTAREA_SMALL);
        return textArea;
    }

}
