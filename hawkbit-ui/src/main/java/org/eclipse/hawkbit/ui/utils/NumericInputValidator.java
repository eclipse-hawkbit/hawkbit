/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;

/**
 * Validator to verify a text input in {@link String} format if it's parseable
 * into an instance of {@link Integer}
 */
public class NumericInputValidator extends AbstractValidator<String> {

    private static final String MESSAGE_FIELD_VALIDATOR_INVALID_INPUT = "message.field.validator.numeric.invalid.input";

    /**
     * Constructor
     * 
     * @param messageSource
     *            needed to fetch the message in case the input is not valid
     */
    public NumericInputValidator(final VaadinMessageSource messageSource) {
        super(messageSource.getMessage(MESSAGE_FIELD_VALIDATOR_INVALID_INPUT));
    }

    @Override
    public ValidationResult apply(final String input, final ValueContext valueContext) {
        try {
            Integer.parseInt(input);
            return ValidationResult.ok();
        } catch (final NumberFormatException e) {
            return this.toResult(input, false);
        }
    }
}
