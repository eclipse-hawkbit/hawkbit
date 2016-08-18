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
    private int maxLengthAllowed;

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
     * Build a textfield
     * 
     * @return textfield
     */
    public E buildTextComponent() {
        final E textComponent = createTextComponent();

        textComponent.setRequired(required);
        textComponent.setImmediate(immediate);

        if (StringUtils.isNotEmpty(caption)) {
            textComponent.setCaption(caption);
        }

        if (StringUtils.isNotEmpty(style)) {
            textComponent.setStyleName(style);
        }

        if (StringUtils.isNotEmpty(styleName)) {
            textComponent.addStyleName(styleName);
        }
        if (StringUtils.isNotEmpty(prompt)) {
            textComponent.setInputPrompt(prompt);
        }

        if (maxLengthAllowed > 0) {
            textComponent.setMaxLength(maxLengthAllowed);
        }

        if (StringUtils.isNotEmpty(id)) {
            textComponent.setId(id);
        }

        return textComponent;
    }

    protected abstract E createTextComponent();

}
