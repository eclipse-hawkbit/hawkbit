/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.exception;

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