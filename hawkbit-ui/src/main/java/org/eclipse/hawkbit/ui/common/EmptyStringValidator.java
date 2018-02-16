/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.validator.StringLengthValidator;

/**
 * Assures that the entered text does not contain only whitespaces. At least one
 * character has to be entered. Leading and trailing whitespaces are allowed as
 * they will be trimmed by the repository.
 */
public class EmptyStringValidator extends StringLengthValidator {

    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_KEY = "validator.textfield.min.length";

    private static final int TEXT_FIELD_DEFAULT_MAX_LENGTH = 64;

    /**
     * Constructor for EmptyStringValidator
     * 
     * @param i18n
     *            {@link VaadinMessageSource}
     */
    public EmptyStringValidator(final VaadinMessageSource i18n) {
        super(i18n.getMessage(MESSAGE_KEY), 1, TEXT_FIELD_DEFAULT_MAX_LENGTH, false);
    }

    /**
     * Constructor for EmptyStringValidator
     * 
     * @param i18n
     *            {@link VaadinMessageSource}
     * @param maxLength
     *            max length of the textfield
     */
    public EmptyStringValidator(final VaadinMessageSource i18n, final int maxLength) {
        super(i18n.getMessage(MESSAGE_KEY), 1, maxLength, false);
    }

    @Override
    public boolean isValid(final Object value) {
        return super.isValid(value != null ? value.toString().trim() : null);
    }

}
