/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.exception;

/**
 * Generic Custom Exception to wrap the Runtime and checked exception
 */
public abstract class AbstractServerRtException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final SpServerError error;

    /**
     * Parameterized constructor.
     * 
     * @param error
     *            detail
     */
    public AbstractServerRtException(final SpServerError error) {
        super(error.getMessage());
        this.error = error;
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            custom error message
     * @param error
     *            detail
     */
    public AbstractServerRtException(final String message, final SpServerError error) {
        super(message);
        this.error = error;
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            custom error message
     * @param error
     *            detail
     * @param cause
     *            of the exception
     */
    public AbstractServerRtException(final String message, final SpServerError error, final Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    /**
     * Parameterized constructor.
     * 
     * @param error
     *            detail
     * @param cause
     *            of the exception
     */
    public AbstractServerRtException(final SpServerError error, final Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
    }

    /**
     * @return the SpServerError which is wrapped by this exception
     */
    public SpServerError getError() {
        return error;
    }
}
