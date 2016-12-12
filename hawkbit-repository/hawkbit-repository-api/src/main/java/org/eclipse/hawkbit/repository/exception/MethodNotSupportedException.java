/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if repository operation is no longer supported.
 *
 */
public final class MethodNotSupportedException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new MethodNotSupportedException with
     * {@link SpServerError#SP_REPO_OPERATION_NOT_SUPPORTED} error.
     */
    public MethodNotSupportedException() {
        super(SpServerError.SP_REPO_OPERATION_NOT_SUPPORTED);
    }

    /**
     * Creates a new MethodNotSupportedException with
     * {@link SpServerError#SP_REPO_OPERATION_NOT_SUPPORTED} error.
     * 
     * @param cause
     *            for the exception
     */
    public MethodNotSupportedException(final Throwable cause) {
        super(SpServerError.SP_REPO_OPERATION_NOT_SUPPORTED, cause);
    }

    /**
     * Creates a new MethodNotSupportedException with
     * {@link SpServerError#SP_REPO_OPERATION_NOT_SUPPORTED} error.
     * 
     * @param message
     *            of the error
     */
    public MethodNotSupportedException(final String message) {
        super(message, SpServerError.SP_REPO_OPERATION_NOT_SUPPORTED);
    }
}
