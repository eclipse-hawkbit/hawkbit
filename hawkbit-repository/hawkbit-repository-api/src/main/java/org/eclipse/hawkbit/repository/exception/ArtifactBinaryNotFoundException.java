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
 *
 *
 *
 */
public final class ArtifactBinaryNotFoundException extends AbstractServerRtException {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link SpServerError#SP_REST_BODY_NOT_READABLE} error.
     */
    public ArtifactBinaryNotFoundException() {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public ArtifactBinaryNotFoundException(final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_LOAD_FAILED, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public ArtifactBinaryNotFoundException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_LOAD_FAILED);
    }
}
