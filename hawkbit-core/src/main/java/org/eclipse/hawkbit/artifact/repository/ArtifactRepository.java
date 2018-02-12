/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.InputStream;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;

/**
 * ArtifactRepository service interface.
 * 
 *
 *
 */
public interface ArtifactRepository {

    /**
     * Stores an artifact into the repository.
     * 
     * @param tenant
     *            the tenant to store the artifact
     * @param content
     *            the content to store
     * @param filename
     *            the filename of the artifact
     * @param contentType
     *            the content type of the artifact
     * @return the stored artifact
     * 
     * @throws MethodNotSupportedException
     *             if implementation does not support the operation
     * @throws ArtifactStoreException
     *             in case storing of the artifact was not successful
     */
    AbstractDbArtifact store(@NotEmpty String tenant, @NotNull InputStream content, @NotEmpty String filename,
            String contentType);

    /**
     * Stores an artifact into the repository.
     * 
     * @param tenant
     *            the tenant to store the artifact
     * @param content
     *            the content to store
     * @param filename
     *            the filename of the artifact
     * @param contentType
     *            the content type of the artifact
     * @param hash
     *            the hashes of the artifact to do hash-checks after storing the
     *            artifact, might be {@code null}
     * @return the stored artifact
     * 
     * @throws MethodNotSupportedException
     *             if implementation does not support the operation
     * @throws ArtifactStoreException
     *             in case storing of the artifact was not successful
     * @throws HashNotMatchException
     *             in case {@code hash} is provided and not matching to the
     *             calculated hashes during storing
     */
    AbstractDbArtifact store(@NotEmpty String tenant, @NotNull InputStream content, @NotEmpty String filename,
            String contentType, DbArtifactHash hash);

    /**
     * Deletes an artifact by its SHA1 hash.
     * 
     * @param tenant
     *            the tenant to store the artifact
     * @param sha1Hash
     *            the sha1-hash of the artifact to delete
     * 
     * @throws MethodNotSupportedException
     *             if implementation does not support the operation
     */
    void deleteBySha1(@NotEmpty String tenant, @NotEmpty String sha1Hash);

    /**
     * Retrieves a {@link AbstractDbArtifact} from the store by it's SHA1 hash.
     * 
     * @param tenant
     *            the tenant to store the artifact
     * @param sha1Hash
     *            the sha1-hash of the file to lookup.
     * @return The artifact file object or {@code null} if no file exists.
     * 
     * @throws MethodNotSupportedException
     *             if implementation does not support the operation
     */
    AbstractDbArtifact getArtifactBySha1(@NotEmpty String tenant, @NotEmpty String sha1Hash);

    /**
     * Deletes all artifacts of given tenant.
     * 
     * @param tenant
     *            to erase
     */
    void deleteByTenant(@NotEmpty String tenant);
}
