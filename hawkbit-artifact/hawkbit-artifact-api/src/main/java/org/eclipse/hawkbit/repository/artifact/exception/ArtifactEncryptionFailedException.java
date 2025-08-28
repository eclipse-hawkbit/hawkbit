/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.exception;

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