/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository.model;

import org.springframework.util.Assert;

/**
 * Database representation of artifact.
 * 
 */
public abstract class AbstractDbArtifact implements DbArtifact {

    private final String artifactId;
    private final long size;
    private final String contentType;

    private DbArtifactHash hashes;

    protected AbstractDbArtifact(final String artifactId, final DbArtifactHash hashes, final long size,
            final String contentType) {
        Assert.notNull(artifactId, "Artifact ID cannot be null");
        Assert.notNull(hashes, "Hashes cannot be null");
        this.artifactId = artifactId;
        this.hashes = hashes;
        this.size = size;
        this.contentType = contentType;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public DbArtifactHash getHashes() {
        return hashes;
    }

    /**
     * Set hashes of the artifact
     * 
     * @param hashes
     *            artifact hashes
     */
    public void setHashes(final DbArtifactHash hashes) {
        this.hashes = hashes;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
