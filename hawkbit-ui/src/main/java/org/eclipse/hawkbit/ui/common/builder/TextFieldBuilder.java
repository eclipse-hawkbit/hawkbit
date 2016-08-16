/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Textfield builder.
 *
 */
public class TextFieldBuilder {

    private static final int TEXT_FIELD_DEFAULT_MAX_LENGTH = 64;

    private String caption;
    private String style;
    private String styleName = ValoTheme.TEXTFIELD_TINY;
    private String prompt;
    private String id;
    private boolean immediate;
    private boolean required;
    private int maxLengthAllowed = TEXT_FIELD_DEFAULT_MAX_LENGTH;

    /**
     * @param caption
     *            the caption to set
     * @return the builder
     */
    public TextFieldBuilder caption(final String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * @param style
     *            the style to set * @return the builder
     * @return the builder
     */
    public TextFieldBuilder style(final String style) {
        this.style = style;
        return this;
    }

    /**
     * @param styleName
     *            the styleName to set
     * @return the builder
     */
    public TextFieldBuilder styleName(final String styleName) {
        this.styleName = styleName;
        return this;
    }

    /**
     * @param required
     *            the required to set
     * @return the builder
     */
    public TextFieldBuilder required(final boolean required) {
        this.required = required;
        return this;
    }

    /**
     * @param prompt
     *            the prompt to set
     * @return the builder
     */
    public TextFieldBuilder prompt(final String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * @param immediate
     *            the immediate to set
     * @return the builder
     */
    public TextFieldBuilder immediate(final boolean immediate) {
        this.immediate = immediate;
        return this;
    }

    /**
     * @param maxLengthAllowed
     *            the maxLengthAllowed to set
     * @return the builder
     */
    public TextFieldBuilder maxLengthAllowed(final int maxLengthAllowed) {
        this.maxLengthAllowed = maxLengthAllowed;
        return this;
    }

    /**
     * @param id
     *            the id to set
     * @return the builder
     */
    public TextFieldBuilder id(final String id) {
        this.id = id;
        return this;
    }

    /**
     * Build a textfield
     * 
     * @return textfield
     */
    public TextField buildTextField() {
        final TextField textField = new TextField();
        textField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        textField.setRequired(required);
        textField.setImmediate(immediate);

        if (StringUtils.isNotEmpty(caption)) {
            textField.setCaption(caption);
        }

        if (StringUtils.isNotEmpty(style)) {
            textField.setStyleName(style);
        }

        if (StringUtils.isNotEmpty(styleName)) {
            textField.addStyleName(styleName);
        }
        if (StringUtils.isNotEmpty(prompt)) {
            textField.setInputPrompt(prompt);
        }

        if (maxLengthAllowed > 0) {
            textField.setMaxLength(maxLengthAllowed);
        }

        if (StringUtils.isNotEmpty(id)) {
            textField.setId(id);
        }

        return textField;
    }

    /**
     * Create a search text field.
     * 
     * @param textChangeListener
     *            listener when text is changed.
     * @return the textfield
     */
    public TextField createSearchField(final TextChangeListener textChangeListener) {
        final TextField textField = style("filter-box").styleName("text-style filter-box-hide").buildTextField();
        textField.setWidth(100.0F, Unit.PERCENTAGE);
        textField.addTextChangeListener(textChangeListener);
        textField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        // 1 seconds timeout.
        textField.setTextChangeTimeout(1000);
        return textField;
    }

}
