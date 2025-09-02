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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.artifact.exception.ArtifactEncryptionUnsupportedException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service responsible for encryption operations. Should be registered as a bean in order its autowired dependencies to be injected.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // singleton holder ensures static access to spring resources in some places
public final class ArtifactEncryptionService {

    private static final ArtifactEncryptionService SINGLETON = new ArtifactEncryptionService();

    private ArtifactEncryption artifactEncryption;
    private ArtifactEncryptionSecretsStorage artifactEncryptionSecretsStore;

    /**
     * @return the artifact encryption service singleton instance
     */
    public static ArtifactEncryptionService getInstance() {
        return SINGLETON;
    }

    @Autowired(required = false) // spring setter injection
    public void setArtifactEncryption(final ArtifactEncryption artifactEncryption) {
        this.artifactEncryption = artifactEncryption;
    }

    @Autowired(required = false) // spring setter injection
    public void setArtifactEncryptionSecretsStore(final ArtifactEncryptionSecretsStorage artifactEncryptionSecretsStore) {
        this.artifactEncryptionSecretsStore = artifactEncryptionSecretsStore;
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
     * Generates encryption secrets and saves them in secret store by id reference.
     *
     * @param id id of the secrets to be generated
     */
    public void addEncryptionSecrets(final long id) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Encryption secrets generation is not supported.");
        }

        final Map<String, String> secrets = artifactEncryption.generateSecrets();
        try {
            secrets.forEach((key, value) -> artifactEncryptionSecretsStore.addSecret(id, key, value));
        } finally {
            // clear secrets from memory as soon as possible
            secrets.clear();
        }
    }

    /**
     * Encrypts artifact stream using the keys retrieved from secrets store by id reference.
     *
     * @param id id of the secrets
     * @param artifactStream artifact stream to encrypt
     * @return encrypted input stream
     */
    public InputStream encryptArtifact(final long id, final InputStream artifactStream) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Artifact encryption is not supported.");
        }

        final Map<String, String> secrets = getEncryptionSecrets(id);
        try {
            return artifactEncryption.encryptStream(secrets, artifactStream);
        } finally {
            // clear secrets from memory as soon as possible
            secrets.clear();
        }
    }

    /**
     * Decrypts artifact stream using the keys retrieved from secrets store by id reference.
     *
     * @param id id of the secrets
     * @param encryptedArtifactStream artifact stream to decrypt
     * @return decrypted input stream
     */
    public InputStream decryptArtifact(final long id, final InputStream encryptedArtifactStream) {
        if (!isEncryptionSupported()) {
            throw new ArtifactEncryptionUnsupportedException("Artifact decryption is not supported.");
        }

        final Map<String, String> secrets = getEncryptionSecrets(id);
        try {
            return artifactEncryption.decryptStream(secrets, encryptedArtifactStream);
        } finally {
            // clear secrets from memory as soon as possible
            secrets.clear();
        }
    }

    /**
     * Size of the underlying encryption algorithm overhead in bytes
     *
     * @return encryption overhead in byte
     */
    public int encryptionSizeOverhead() {
        return artifactEncryption.encryptionSizeOverhead();
    }

    private Map<String, String> getEncryptionSecrets(final long id) {
        final Set<String> requiredSecretsKeys = artifactEncryption.requiredSecretKeys();
        final Map<String, String> requiredSecrets = new HashMap<>();
        for (final String requiredSecretsKey : requiredSecretsKeys) {
            final Optional<String> requiredSecretsValue = artifactEncryptionSecretsStore.getSecret(id, requiredSecretsKey);
            requiredSecretsValue.ifPresent(secretValue -> requiredSecrets.put(requiredSecretsKey, secretValue));
        }
        return requiredSecrets;
    }
}