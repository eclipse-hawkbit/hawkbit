/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import com.vaadin.data.validator.RegexpValidator;

/**
 * Assures that the entered text does not contain only whitespaces. At least one
 * character has to be entered. Leading and trailing whitespaces are allowed as
 * they will be trimmed by the repository.
 */
public class EmptyStringValidator extends RegexpValidator {

    private static final long serialVersionUID = 1L;

    static String regex = "[\\s]*(\\w[\\s,-.;:/]*)+[\\s]*";

    /**
     * Validator for validating if at least one character has entered
     * 
     * @param errorMessage
     *            the message to display if validation fails
     */
    public EmptyStringValidator(final String errorMessage) {
        super(regex, true, errorMessage);
    }

}
