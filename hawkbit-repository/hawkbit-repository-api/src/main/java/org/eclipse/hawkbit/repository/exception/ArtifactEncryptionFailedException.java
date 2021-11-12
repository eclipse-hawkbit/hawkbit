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
 * Exception being thrown in case of error while generating encryption secrets,
 * encrypting or decrypting artifacts.
 */
public final class ArtifactEncryptionFailedException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    private final EncryptionOperation encryptionOperation;

    public ArtifactEncryptionFailedException(final EncryptionOperation encryptionOperation) {
        this(encryptionOperation, null, null);
    }

    public ArtifactEncryptionFailedException(final EncryptionOperation encryptionOperation, final String message) {
        this(encryptionOperation, message, null);
    }

    public ArtifactEncryptionFailedException(final EncryptionOperation encryptionOperation, final String message,
            final Throwable cause) {
        super(message, SpServerError.SP_ARTIFACT_ENCRYPTION_FAILED, cause);
        this.encryptionOperation = encryptionOperation;
    }

    public EncryptionOperation getEncryptionOperation() {
        return encryptionOperation;
    }

    /**
     * Encryption operation that caused the exception.
     */
    public enum EncryptionOperation {
        GENERATE_SECRETS, ENCRYPT, DECRYPT;
    }

}
