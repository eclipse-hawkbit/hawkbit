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

public final class ArtifactBinaryNotFoundException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_ARTIFACT_LOAD_FAILED} error.
     */
    public ArtifactBinaryNotFoundException() {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED);
    }

    /**
     * @param cause for the exception
     */
    public ArtifactBinaryNotFoundException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED, cause);
    }

    /**
     * @param message of the error
     */
    public ArtifactBinaryNotFoundException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_LOAD_FAILED);
    }
}
