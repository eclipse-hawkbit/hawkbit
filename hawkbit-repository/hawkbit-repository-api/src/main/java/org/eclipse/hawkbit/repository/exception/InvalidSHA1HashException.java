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
 * Thrown if SHA1 checksum check fails.
 *
 *
 *
 *
 */
public class InvalidSHA1HashException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH} error.
     */
    public InvalidSHA1HashException() {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH);
    }

    /**
     * @param message
     *            of the error
     * @param cause
     *            for the exception
     */
    public InvalidSHA1HashException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public InvalidSHA1HashException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH);
    }

}
