/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import java.util.LinkedList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.vaadin.data.Validator;
import com.vaadin.ui.AbstractTextField;

/**
 * Abstract Text field builder.
 *
 * @param <E>
 *            the concrete text component
 */
public abstract class AbstractTextFieldBuilder<E extends AbstractTextField> {

    private String caption;
    private String style;
    private String styleName;
    private String prompt;
    private String id;
    private boolean immediate;
    private boolean required;
    private boolean readOnly;
    private boolean enabled = true;
    private int maxLengthAllowed;
    private final List<Validator> validators = new LinkedList<>();

    /**
     * @param caption
     *            the caption to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> caption(final String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * @param style
     *            the style to set * @return the builder
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> style(final String style) {
        this.style = style;
        return this;
    }

    /**
     * @param styleName
     *            the styleName to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> styleName(final String styleName) {
        this.styleName = styleName;
        return this;
    }

    /**
     * @param required
     *            the required to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> required(final boolean required) {
        this.required = required;
        return this;
    }

    /**
     * @param readOnly
     *            the readOnly to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> readOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * @param enabled
     *            the enabled to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> enabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * @param prompt
     *            the prompt to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> prompt(final String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * @param immediate
     *            the immediate to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> immediate(final boolean immediate) {
        this.immediate = immediate;
        return this;
    }

    /**
     * @param maxLengthAllowed
     *            the maxLengthAllowed to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> maxLengthAllowed(final int maxLengthAllowed) {
        this.maxLengthAllowed = maxLengthAllowed;
        return this;
    }

    /**
     * @param id
     *            the id to set
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> id(final String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * @param validator
     *            the validator to set for this field
     * @return the builder
     */
    public AbstractTextFieldBuilder<E> validator(final Validator validator) {
        validators.add(validator);
        return this;
    }

    /**
     * Build a textfield
     * 
     * @return textfield
     */
    public E buildTextComponent() {
        final E textComponent = createTextComponent();

        textComponent.setRequired(required);
        textComponent.setImmediate(immediate);
        textComponent.setReadOnly(readOnly);
        textComponent.setEnabled(enabled);

        if (!StringUtils.isEmpty(caption)) {
            textComponent.setCaption(caption);
        }

        if (!StringUtils.isEmpty(style)) {
            textComponent.setStyleName(style);
        }

        if (!StringUtils.isEmpty(styleName)) {
            textComponent.addStyleName(styleName);
        }
        if (!StringUtils.isEmpty(prompt)) {
            textComponent.setInputPrompt(prompt);
        }

        if (maxLengthAllowed > 0) {
            textComponent.setMaxLength(maxLengthAllowed);
        }

        if (!StringUtils.isEmpty(id)) {
            textComponent.setId(id);
        }

        if (!validators.isEmpty()) {
            validators.forEach(textComponent::addValidator);
        }

        return textComponent;
    }

    protected abstract E createTextComponent();

}
