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

import com.vaadin.data.Validator;
import com.vaadin.data.validator.StringLengthValidator;

/**
 * Assures that the entered text does not contain only whitespaces. At least one
 * character has to be entered. Leading and trailing whitespaces are allowed as
 * they will be trimmed by the repository.
 */
public class EmptyStringValidator implements Validator {

    private static final long serialVersionUID = 1L;

    private final VaadinMessageSource i18n;

    /**
     * Constructor
     * 
     * @param vaadinMessageSource
     *            {@link VaadinMessageSource}
     */
    public EmptyStringValidator(final VaadinMessageSource vaadinMessageSource) {
        this.i18n = vaadinMessageSource;
    }

    @Override
    public void validate(final Object value) {
        new StringLengthValidator(i18n.getMessage("validator.textfield.min.length"), 1, Integer.MAX_VALUE, false)
                .validate(((String) value).trim());
    }
}
