/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.InputStream;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;

/**
 * ArtifactRepository service interface.
 */
public interface ArtifactRepository {

    /**
     * Stores an artifact into the repository.
     *
     * @param tenant the tenant to store the artifact
     * @param content the content to store
     * @param filename the filename of the artifact
     * @param contentType the content type of the artifact
     * @param hash the hashes of the artifact to do hash-checks after storing the artifact, might be {@code null}
     * @return the stored artifact
     * @throws UnsupportedOperationException if implementation does not support the operation
     * @throws ArtifactStoreException in case storing of the artifact was not successful
     * @throws HashNotMatchException in case {@code hash} is provided and not matching to the calculated hashes during storing
     */
    AbstractDbArtifact store(
            @NotEmpty String tenant, @NotNull InputStream content, @NotEmpty String filename,
            String contentType, DbArtifactHash hash);

    /**
     * Deletes an artifact by its SHA1 hash.
     *
     * @param tenant the tenant to store the artifact
     * @param sha1Hash the sha1-hash of the artifact to delete
     * @throws UnsupportedOperationException if implementation does not support the operation
     */
    void deleteBySha1(@NotEmpty String tenant, @NotEmpty String sha1Hash);

    /**
     * Retrieves a {@link AbstractDbArtifact} from the store by its SHA1 hash.
     *
     * @param tenant the tenant to store the artifact
     * @param sha1Hash the sha1-hash of the file to lookup.
     * @return The artifact file object or {@code null} if no file exists.
     * @throws UnsupportedOperationException if implementation does not support the operation
     */
    AbstractDbArtifact getArtifactBySha1(@NotEmpty String tenant, @NotEmpty String sha1Hash);

    /**
     * Deletes all artifacts of given tenant.
     *
     * @param tenant to erase
     */
    void deleteByTenant(@NotEmpty String tenant);

    /**
     * Checks if an artifact exists for a given tenant by its sha1 hash
     *
     * @param tenant the tenant
     * @param sha1Hash the sha1-hash of the file to lookup.
     * @return the boolean whether the artifact exists or not
     */
    boolean existsByTenantAndSha1(@NotEmpty String tenant, @NotEmpty String sha1Hash);
}