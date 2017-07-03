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

/**
 * Database representation of artifact.
 * 
 */
public abstract class AbstractDbArtifact {

    private final String artifactId;
    private final DbArtifactHash hashes;
    private final Long size;
    private final String contentType;

    protected AbstractDbArtifact(final String artifactId, final DbArtifactHash hashes, final Long size,
            final String contentType) {
        this.artifactId = artifactId;
        this.hashes = hashes;
        this.size = size;
        this.contentType = contentType;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public DbArtifactHash getHashes() {
        return hashes;
    }

    public Long getSize() {
        return size;
    }

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
