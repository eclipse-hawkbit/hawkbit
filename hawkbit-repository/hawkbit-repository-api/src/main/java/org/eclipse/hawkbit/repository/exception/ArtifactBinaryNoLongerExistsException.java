/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Exception indicating that an artifact's binary does not exist anymore. This
 * might be caused due to the soft deletion of a {@link SoftwareModule}.
 *
 */
public class ArtifactBinaryNoLongerExistsException extends AbstractServerRtException {
    private static final SpServerError THIS_ERROR = SpServerError.SP_ARTIFACT_BINARY_DELETED;

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new ArtifactBinaryGoneException error.
     */
    public ArtifactBinaryNoLongerExistsException() {
        super(THIS_ERROR);
    }

    /**
     * Creates a new ArtifactBinaryGoneException error with cause.
     * 
     * @param cause
     *            for the exception
     */
    public ArtifactBinaryNoLongerExistsException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Creates a new ArtifactBinaryGoneException error with message.
     * 
     * @param message
     *            of the error
     */
    public ArtifactBinaryNoLongerExistsException(final String message) {
        super(message, THIS_ERROR);
    }

}
