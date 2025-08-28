/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

import java.io.Serial;

/**
 * {@link ArtifactStoreException} is thrown in case storing of an artifact was not successful.
 */
public class ArtifactStoreException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public  ArtifactStoreException(final String message) {
        this(message, null);
    }

    public ArtifactStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }
}