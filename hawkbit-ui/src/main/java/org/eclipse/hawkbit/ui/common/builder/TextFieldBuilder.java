/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.builder;

import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Textfield builder.
 *
 */
public class TextFieldBuilder extends AbstractTextFieldBuilder<TextFieldBuilder, TextField> {
    private static final int INPUT_DEBOUNCE_TIMEOUT = 500;

    /**
     * Constructor.
     * 
     * @param maxLengthAllowed
     *            as mandatory field
     */
    public TextFieldBuilder(final int maxLengthAllowed) {
        super(maxLengthAllowed);
    }

    @Override
    protected TextField createTextComponent() {
        final TextField textField = new TextField();
        textField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(INPUT_DEBOUNCE_TIMEOUT);

        return textField;
    }

}
