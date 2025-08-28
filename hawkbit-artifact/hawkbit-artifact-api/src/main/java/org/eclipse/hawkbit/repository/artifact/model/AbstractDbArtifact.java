/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.model;

import java.util.Objects;

import lombok.Data;

/**
 * Database representation of artifact.
 */
@Data
public abstract class AbstractDbArtifact implements DbArtifact {

    private final String artifactId;
    private final long size;
    private final String contentType;

    private DbArtifactHash hashes;

    protected AbstractDbArtifact(final String artifactId, final DbArtifactHash hashes, final long size, final String contentType) {
        this.artifactId = Objects.requireNonNull(artifactId, "Artifact ID cannot be null");
        this.hashes = Objects.requireNonNull(hashes, "Hashes cannot be null");
        this.size = size;
        this.contentType = contentType;
    }
}