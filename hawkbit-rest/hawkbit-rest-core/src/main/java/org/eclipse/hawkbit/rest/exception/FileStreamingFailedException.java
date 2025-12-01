/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if artifact content streaming to client failed.
 */
public final class FileStreamingFailedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor with error string.
     *
     * @param message of the error
     */
    public FileStreamingFailedException(final String message) {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED, message);
    }

    /**
     * Constructor with error string and cause.
     *
     * @param message of the error
     * @param cause for the exception
     */
    public FileStreamingFailedException(final String message, final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED, message, cause);
    }
}