/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.exception;

/**
 * {@link GenericSpServerException} is thrown when a given entity in's actual
 * and cannot be stored within the current session. Reason could be that it has
 * been changed within another session.
 * 
 *
 *
 */
public class GenericSpServerException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;
    private static final SpServerError THIS_ERROR = SpServerError.SP_REPO_GENERIC_ERROR;

    /**
     * Constructor.
     */
    public GenericSpServerException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public GenericSpServerException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            custom error message
     * @param cause
     *            of the exception
     */
    public GenericSpServerException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            custom error message
     */
    public GenericSpServerException(final String message) {
        super(message, THIS_ERROR);
    }

}
