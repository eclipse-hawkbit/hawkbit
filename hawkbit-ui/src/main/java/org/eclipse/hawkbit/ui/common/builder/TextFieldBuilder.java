/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Textfield builder.
 *
 */
public class TextFieldBuilder extends AbstractTextFieldBuilder<TextField> {

    /**
     * Constructor.
     */
    public TextFieldBuilder() {
        this(null);
    }

    /**
     * Constructor.
     * 
     * @param id
     *            the id
     */
    public TextFieldBuilder(final String id) {
        styleName(ValoTheme.TEXTAREA_TINY);
        id(id);
    }

    /**
     * Create a search text field.
     * 
     * @param textChangeListener
     *            listener when text is changed.
     * @return the textfield
     */
    public TextField createSearchField(final TextChangeListener textChangeListener) {
        final TextField textField = style("filter-box").styleName("text-style filter-box-hide").buildTextComponent();
        textField.setWidth(100.0F, Unit.PERCENTAGE);
        textField.addTextChangeListener(textChangeListener);
        textField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        // 1 seconds timeout.
        textField.setTextChangeTimeout(1000);
        return textField;
    }

    @Override
    protected TextField createTextComponent() {
        final TextField textField = new TextField();
        textField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        return textField;
    }

}
