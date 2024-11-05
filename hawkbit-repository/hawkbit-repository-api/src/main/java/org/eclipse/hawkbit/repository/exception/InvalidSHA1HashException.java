/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if SHA1 checksum check fails.
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
     * @param message of the error
     * @param cause for the exception
     */
    public InvalidSHA1HashException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH, cause);
    }

    /**
     * @param message of the error
     */
    public InvalidSHA1HashException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_SHA1_MATCH);
    }

}
