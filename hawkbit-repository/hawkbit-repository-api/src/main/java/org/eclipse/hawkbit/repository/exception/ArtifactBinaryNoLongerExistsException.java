/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
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
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Exception indicating that an artifact's binary does not exist anymore. This
 * might be caused due to the soft deletion of a {@link SoftwareModule}.
 */
public class ArtifactBinaryNoLongerExistsException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_ARTIFACT_BINARY_DELETED;

    /**
     * Creates a new ArtifactBinaryGoneException error.
     */
    public ArtifactBinaryNoLongerExistsException() {
        super(THIS_ERROR);
    }

    /**
     * Creates a new ArtifactBinaryGoneException error with cause.
     *
     * @param cause for the exception
     */
    public ArtifactBinaryNoLongerExistsException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Creates a new ArtifactBinaryGoneException error with message.
     *
     * @param message of the error
     */
    public ArtifactBinaryNoLongerExistsException(final String message) {
        super(message, THIS_ERROR);
    }

}
