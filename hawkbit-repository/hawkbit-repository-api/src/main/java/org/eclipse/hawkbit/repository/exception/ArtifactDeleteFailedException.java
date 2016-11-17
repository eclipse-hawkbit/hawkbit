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
 * Thrown if artifact deletion failed.
 *
 *
 *
 *
 */
public final class ArtifactDeleteFailedException extends AbstractServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_REST_BODY_NOT_READABLE} error.
     */
    public ArtifactDeleteFailedException() {
        super(SpServerError.SP_ARTIFACT_DELETE_FAILED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public ArtifactDeleteFailedException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_DELETE_FAILED, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public ArtifactDeleteFailedException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_DELETE_FAILED);
    }
}
