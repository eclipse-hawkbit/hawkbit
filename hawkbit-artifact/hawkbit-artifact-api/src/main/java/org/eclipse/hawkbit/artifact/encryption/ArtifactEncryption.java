/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.encryption;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.artifact.exception.ArtifactEncryptionFailedException;

/**
 * Interface definition for artifact encryption.
 */
public interface ArtifactEncryption {

    /**
     * Defines the required secret keys for particular encryption algorithm.
     *
     * @return set of required secret keys
     */
    Set<String> requiredSecretKeys();

    /**
     * Generates required secrets key/value pairs.
     *
     * @return secrets key/value pairs
     * @throws ArtifactEncryptionFailedException thrown in case of an error while generating secrets
     */
    Map<String, String> generateSecrets();

    /**
     * Encrypts artifact stream with provided secrets.
     *
     * @param secrets secrets key/value pairs to be used for encryption
     * @param stream artifact stream to encrypt
     * @return encrypted input stream
     * @throws ArtifactEncryptionFailedException thrown in case of an error while encrypting the provided stream
     */
    InputStream encryptStream(final Map<String, String> secrets, final InputStream stream);

    /**
     * Decrypts encrypted artifact stream based on provided secrets.
     *
     * @param secrets secrets key/value pairs to be used for decryption
     * @param stream artifact stream to decrypt
     * @return decrypted input stream
     * @throws ArtifactEncryptionFailedException thrown in case of an error while decrypting the provided stream
     */
    InputStream decryptStream(final Map<String, String> secrets, final InputStream stream);

    /**
     * Size of the underlying encryption algorithm overhead in bytes
     *
     * @return encryption overhead in byte
     */
    int encryptionSizeOverhead();
}