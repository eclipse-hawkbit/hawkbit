/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Interface definition for artifact encryption secrets store.
 *
 */
public interface ArtifactEncryptionSecretsStore {

    /**
     * Adds secret key/value pair associated with particular
     * {@link SoftwareModule} id to the store.
     *
     * @param softwareModuleId
     *            {@link SoftwareModule} id associated with the secret
     * @param secretKey
     *            key of the secret
     * @param secretValue
     *            value of the secret
     */
    void addSecret(final long softwareModuleId, final String secretKey, final String secretValue);

    /**
     * Checks if secret is present for particular {@link SoftwareModule} id and
     * key in the store.
     *
     * @param softwareModuleId
     *            {@link SoftwareModule} id associated with the secret
     * @param secretKey
     *            key of the secret
     */
    boolean secretExists(final long softwareModuleId, final String secretKey);

    /**
     * Retrieves secret value associated with particular {@link SoftwareModule}
     * id and key from the store.
     *
     * @param softwareModuleId
     *            {@link SoftwareModule} id associated with the secret
     * @param secretKey
     *            key of the secret
     */
    Optional<String> getSecret(final long softwareModuleId, final String secretKey);

    /**
     * Removes secret key/value pair associated with particular
     * {@link SoftwareModule} id from the store.
     *
     * @param softwareModuleId
     *            {@link SoftwareModule} id associated with the secret
     * @param secretKey
     *            key of the secret
     */
    void removeSecret(final long softwareModuleId, final String secretKey);
}
