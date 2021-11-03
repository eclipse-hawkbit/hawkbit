/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

/**
 *
 * Exception being thrown in case of error while generating encryption secrets,
 * encrypting or decrypting artifacts.
 *
 */
public final class ArtifactEncryptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final EncryptionOperation encryptionOperation;

    public ArtifactEncryptionException(final EncryptionOperation encryptionOperation) {
        this(encryptionOperation, null);
    }

    public ArtifactEncryptionException(final EncryptionOperation encryptionOperation, final Throwable cause) {
        super(cause);
        this.encryptionOperation = encryptionOperation;
    }

    public EncryptionOperation getEncryptionOperation() {
        return encryptionOperation;
    }

    public enum EncryptionOperation {
        NOT_SUPPORTED, GENERATE_KEYS, ENCRYPT, DECRYPT;
    }

}
