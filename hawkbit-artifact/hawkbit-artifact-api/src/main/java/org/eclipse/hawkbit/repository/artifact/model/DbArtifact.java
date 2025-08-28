/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.model;

import java.io.InputStream;

/**
 * Interface definition for artifact binary.
 */
public interface DbArtifact {

    /**
     * @return ID of the artifact
     */
    String getArtifactId();

    /**
     * @return hashes of the artifact
     */
    DbArtifactHash getHashes();

    /**
     * @return size of the artifact in bytes
     */
    long getSize();

    /**
     * @return content-type if known by the repository or <code>null</code>
     */
    String getContentType();

    /**
     * Creates an {@link InputStream} on this artifact. Caller has to take care of
     * closing the stream. Repeatable calls open a new {@link InputStream}.
     *
     * @return {@link InputStream} to read from artifact.
     */
    InputStream getFileInputStream();
}