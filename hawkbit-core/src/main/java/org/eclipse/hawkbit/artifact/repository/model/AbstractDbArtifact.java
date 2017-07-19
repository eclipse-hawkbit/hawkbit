/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository.model;

import java.io.InputStream;

import org.springframework.util.Assert;

/**
 * Database representation of artifact.
 * 
 */
public abstract class AbstractDbArtifact {

    private final String artifactId;
    private final DbArtifactHash hashes;
    private final long size;
    private final String contentType;

    protected AbstractDbArtifact(final String artifactId, final DbArtifactHash hashes, final long size,
            final String contentType) {
        Assert.notNull(artifactId, "Artifact ID cannot be null");
        Assert.notNull(hashes, "Hashes cannot be null");
        this.artifactId = artifactId;
        this.hashes = hashes;
        this.size = size;
        this.contentType = contentType;
    }

    /**
     * @return ID of the artifact
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return hashes of the artifact
     */
    public DbArtifactHash getHashes() {
        return hashes;
    }

    /**
     * @return site of the artifact in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * @return content-type if known by the repository or <code>null</code>
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Creates an {@link InputStream} on this artifact. Caller has to take care
     * of closing the stream. Repeatable calls open a new {@link InputStream}.
     * 
     * @return {@link InputStream} to read from artifact.
     */
    public abstract InputStream getFileInputStream();
}
