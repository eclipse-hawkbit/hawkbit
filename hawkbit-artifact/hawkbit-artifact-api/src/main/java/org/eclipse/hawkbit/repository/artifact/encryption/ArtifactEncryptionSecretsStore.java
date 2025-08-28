/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.encryption;

import java.util.Optional;

/**
 * Interface definition for artifact encryption secrets store. It maintains secret key/value pairs
 * identified by id (e.g. software module id)
 */
public interface ArtifactEncryptionSecretsStore {

    /**
     * Adds secret key/value pair associated with particular id (e.g. software module id) to the store.
     *
     * @param id id of the secret
     * @param secretKey key of the secret
     * @param secretValue value of the secret
     */
    void addSecret(final long id, final String secretKey, final String secretValue);

    /**
     * Retrieves secret value associated with particular id and key from the store.
     *
     * @param id id of the secret
     * @param secretKey key of the secret
     */
    Optional<String> getSecret(final long id, final String secretKey);
}