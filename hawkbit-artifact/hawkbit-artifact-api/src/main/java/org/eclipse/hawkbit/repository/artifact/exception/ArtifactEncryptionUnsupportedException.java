/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

import java.io.Serial;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception being thrown when artifact encryption is not supported
 */
public final class ArtifactEncryptionUnsupportedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ArtifactEncryptionUnsupportedException(final String message) {
        super(SpServerError.SP_ARTIFACT_ENCRYPTION_NOT_SUPPORTED, message);
    }
}