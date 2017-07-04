/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import java.util.stream.Collectors;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * the {@link ConstraintViolationException} is thrown when an entity is tried to
 * be saved which has constraint violations
 */
public class ConstraintViolationException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_FORMATTER_SEPARATOR = " ";

    /**
     * Constructor for {@link Throwable}
     * 
     * @param ex
     *            the cause
     */
    public ConstraintViolationException(final Throwable ex) {
        this(getExceptionMessage(ex));
    }

    /**
     * Creates a new {@link ConstraintViolationException} with the error code
     * {@link SpServerError#SP_REPO_CONSTRAINT_VIOLATION}.
     * 
     * @param msgText
     *            the message text for this exception
     */
    public ConstraintViolationException(final String msgText) {
        super(msgText, SpServerError.SP_REPO_CONSTRAINT_VIOLATION);
    }

    /**
     * Uses the information of
     * {@link javax.validation.ConstraintViolationException} to provide a proper
     * error message for {@link ConstraintViolationException}
     * 
     * @param ex
     *            javax.validation.ConstraintViolationException which is thrown
     * @return message String with proper error information
     */
    public static String getExceptionMessage(final Throwable ex) {
        if (ex instanceof javax.validation.ConstraintViolationException) {
            return ((javax.validation.ConstraintViolationException) ex)
                    .getConstraintViolations().stream().map(violation -> violation.getPropertyPath()
                            + MESSAGE_FORMATTER_SEPARATOR + violation.getMessage() + ".")
                    .collect(Collectors.joining(MESSAGE_FORMATTER_SEPARATOR));
        }

        return SpServerError.SP_REPO_CONSTRAINT_VIOLATION.getMessage();
    }

}
