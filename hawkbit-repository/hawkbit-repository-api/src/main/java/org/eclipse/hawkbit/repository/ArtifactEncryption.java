/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionFailedException;

/**
 * Interface definition for artifact encryption.
 *
 */
public interface ArtifactEncryption {

    /**
     * Defines the required secret keys for particular encryption algorithm.
     *
     * @return list of required secret keys
     */
    Set<String> requiredSecretKeys();

    /**
     * Generates required secrets key/value pairs.
     *
     * @return secrets key/value pairs
     * @throws ArtifactEncryptionFailedException
     *             thrown in case of an error while generating secrets
     */
    Map<String, String> generateSecrets();

    /**
     * Encrypts artifact stream with provided secrets.
     *
     * @param secrets
     *            secrets key/value pairs to be used for encryption
     * @param stream
     *            artifact stream to encrypt
     * @return encrypted input stream
     * @throws ArtifactEncryptionFailedException
     *             thrown in case of an error while encrypting the provided stream
     */
    InputStream encryptStream(final Map<String, String> secrets, final InputStream stream);

    /**
     * Decrypts encrypted artifact stream based on provided secrets.
     *
     * @param secrets
     *            secrets key/value pairs to be used for decryption
     * @param stream
     *            artifact stream to decrypt
     * @return decrypted input stream
     * @throws ArtifactEncryptionFailedException
     *             thrown in case of an error while decrypting the provided stream
     */
    InputStream decryptStream(final Map<String, String> secrets, final InputStream stream);

    /**
     * Size of the underlying encryption algorithm overhead in bytes
     * 
     * @return encryption overhead in byte
     */
    int encryptionSizeOverhead();
}
