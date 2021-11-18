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

/**
 * Exception being thrown when artifact encryption is not supported
 */
public final class ArtifactEncryptionUnsupportedException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public ArtifactEncryptionUnsupportedException() {
        super(SpServerError.SP_ARTIFACT_ENCRYPTION_NOT_SUPPORTED);
    }

    /**
     * @param message
     *            of the error
     */
    public ArtifactEncryptionUnsupportedException(final String message) {
        super(message, SpServerError.SP_ARTIFACT_ENCRYPTION_NOT_SUPPORTED);
    }
}
