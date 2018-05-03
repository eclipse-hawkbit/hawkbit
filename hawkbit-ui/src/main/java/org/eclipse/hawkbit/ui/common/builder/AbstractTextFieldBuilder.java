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

import org.eclipse.hawkbit.ui.common.EmptyStringValidator;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;

import com.vaadin.data.Validator;
import com.vaadin.ui.AbstractTextField;

/**
 * Abstract Text field builder.
 * 
 * @param <T>
 *            type of the builder
 * @param <E>
 *            the text component
 *
 */
public abstract class AbstractTextFieldBuilder<T, E extends AbstractTextField> {

    private String caption;
    private String style;
    private String styleName;
    private String prompt;
    private String id;
    private boolean immediate = true;
    private boolean required;
    private boolean readOnly;
    private boolean enabled = true;
    private final int maxLengthAllowed;
    private final List<Validator> validators = new LinkedList<>();

    protected AbstractTextFieldBuilder(final int maxLengthAllowed) {
        this.maxLengthAllowed = maxLengthAllowed;
    }

    /**
     * @param caption
     *            the caption to set
     * @return the builder
     */
    public T caption(final String caption) {
        this.caption = caption;
        this.prompt = caption;
        return (T) this;
    }

    /**
     * @param style
     *            the style to set * @return the builder
     * @return the builder
     */
    public T style(final String style) {
        this.style = style;
        return (T) this;
    }

    /**
     * @param styleName
     *            the styleName to set
     * @return the builder
     */
    public T styleName(final String styleName) {
        this.styleName = styleName;
        return (T) this;
    }

    /**
     * @param required
     *            the required to set
     * @param i18n
     *            to translate error message
     * @return the builder
     */
    public T required(final boolean required, final VaadinMessageSource i18n) {
        this.required = required;
        if (required) {
            validators.add(new EmptyStringValidator(i18n, maxLengthAllowed));
        }
        return (T) this;
    }

    /**
     * @param readOnly
     *            the readOnly to set
     * @return the builder
     */
    public T readOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        return (T) this;
    }

    /**
     * @param enabled
     *            the enabled to set
     * @return the builder
     */
    public T enabled(final boolean enabled) {
        this.enabled = enabled;
        return (T) this;
    }

    /**
     * @param prompt
     *            the prompt to set
     * @return the builder
     */
    public T prompt(final String prompt) {
        this.prompt = prompt;
        return (T) this;
    }

    /**
     * @param immediate
     *            the immediate to set
     * @return the builder
     */
    public T immediate(final boolean immediate) {
        this.immediate = immediate;
        return (T) this;
    }

    /**
     * @param id
     *            the id to set
     * @return the builder
     */
    public T id(final String id) {
        this.id = id;
        return (T) this;
    }

    /**
     * 
     * @param validator
     *            the validator to set for this field
     * @return the builder
     */
    public T validator(final Validator validator) {
        validators.add(validator);
        return (T) this;
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

        textComponent.setNullRepresentation("");

        return textComponent;
    }

    protected abstract E createTextComponent();

}
