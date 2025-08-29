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

import java.util.Optional;

/**
 * Interface definition for artifact encryption secrets store. It maintains secret key/value pairs
 * identified by id (e.g. software module id)
 */
public interface ArtifactEncryptionSecretsStorage {

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