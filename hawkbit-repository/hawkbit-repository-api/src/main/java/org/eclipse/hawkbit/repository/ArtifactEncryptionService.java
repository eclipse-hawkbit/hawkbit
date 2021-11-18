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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.exception.ArtifactEncryptionUnsupportedException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service responsible for encryption operations.
 *
 */
public final class ArtifactEncryptionService {

    private static final ArtifactEncryptionService SINGLETON = new ArtifactEncryptionService();

    @Autowired(required = false)
    private ArtifactEncryption artifactEncryption;

    @Autowired(required = false)
    private ArtifactEncryptionSecretsStore artifactEncryptionSecretsStore;

    private ArtifactEncryptionService() {
    }

    /**
     * @return the artifact encryption service singleton instance
     */
    public static ArtifactEncryptionService getInstance() {
        return SINGLETON;
    }

    /**
     * Checks if required encryption and secrets store beans are present.
     *
     * @return if encryption is supported
     */
    public boolean isEncryptionSupported() {
        return artifactEncryption != null && artifactEncryptionSecretsStore != null;
    }

    /**
     * Generates encryption secrets and saves them in secret store by software
     * module id reference.
     *
     * @param smId
     *            software module id
     */
    public void addSoftwareModuleEncryptionSecrets(final long smId) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Encryption secrets generation is not supported.");
        }

        final Map<String, String> secrets = artifactEncryption.generateSecrets();
        secrets.forEach((key, value) -> artifactEncryptionSecretsStore.addSecret(smId, key, value));
        // we want to clear secrets from memory as soon as possible
        secrets.clear();
    }

    /**
     * Encrypts artifact stream using the keys retrieved from secrets store by
     * software module id reference.
     *
     * @param smId
     *            software module id
     * @param artifactStream
     *            artifact stream to encrypt
     * @return encrypted input stream
     */
    public InputStream encryptSoftwareModuleArtifact(final long smId, final InputStream artifactStream) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Artifact encryption is not supported.");
        }

        return artifactEncryption.encryptStream(getSoftwareModuleEncryptionSecrets(smId), artifactStream);
    }

    private Map<String, String> getSoftwareModuleEncryptionSecrets(final long smId) {
        final Set<String> requiredSecretsKeys = artifactEncryption.requiredSecretKeys();
        final Map<String, String> requiredSecrets = new HashMap<>();
        for (final String requiredSecretsKey : requiredSecretsKeys) {
            final Optional<String> requiredSecretsValue = artifactEncryptionSecretsStore.getSecret(smId,
                    requiredSecretsKey);
            requiredSecretsValue.ifPresent(secretValue -> requiredSecrets.put(requiredSecretsKey, secretValue));
        }

        return requiredSecrets;
    }

    /**
     * Decrypts artifact stream using the keys retrieved from secrets store by
     * software module id reference.
     *
     * @param smId
     *            software module id
     * @param encryptedArtifactStream
     *            artifact stream to decrypt
     * @return decrypted input stream
     */
    public InputStream decryptSoftwareModuleArtifact(final long smId, final InputStream encryptedArtifactStream) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Artifact decryption is not supported.");
        }

        return artifactEncryption.decryptStream(getSoftwareModuleEncryptionSecrets(smId), encryptedArtifactStream);
    }

    /**
     * Size of the underlying encryption algorithm overhead in bytes
     *
     * @return encryption overhead in byte
     */
    public int encryptionSizeOverhead() {
        return artifactEncryption.encryptionSizeOverhead();
    }
}
