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

public final class ArtifactUploadFailedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_ARTIFACT_UPLOAD_FAILED} error.
     */
    public ArtifactUploadFailedException() {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED);
    }

    /**
     * @param cause for the exception
     */
    public ArtifactUploadFailedException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_UPLOAD_FAILED, cause);
    }

    /**
     * @param message of the error
     */
    public ArtifactUploadFailedException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED);
    }

    /**
     * @param message for the error
     * @param cause of the error
     */
    public ArtifactUploadFailedException(final String message, final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_UPLOAD_FAILED, cause);
    }

}
