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

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Thrown if MD5 checksum check fails.
 */
public class InvalidMD5HashException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH} error.
     */
    public InvalidMD5HashException() {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH);
    }

    /**
     * @param message of the error
     * @param cause for the exception
     */
    public InvalidMD5HashException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH, cause);
    }

    /**
     * @param message of the error
     */
    public InvalidMD5HashException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED_MD5_MATCH);
    }

}
