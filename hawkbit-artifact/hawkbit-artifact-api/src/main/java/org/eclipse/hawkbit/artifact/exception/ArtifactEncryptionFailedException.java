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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * Exception being thrown in case of error while generating encryption secrets, encrypting or decrypting artifacts.
 */
@EqualsAndHashCode(callSuper = true)
public final class ArtifactEncryptionFailedException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Encryption operation that caused the exception.
     */
    public enum EncryptionOperation {
        GENERATE_SECRETS, ENCRYPT, DECRYPT;
    }

    @Getter
    private final EncryptionOperation encryptionOperation;

    public ArtifactEncryptionFailedException(final EncryptionOperation encryptionOperation, final String message, final Throwable cause) {
        super(SpServerError.SP_ARTIFACT_ENCRYPTION_FAILED, message, cause);
        this.encryptionOperation = encryptionOperation;
    }
}