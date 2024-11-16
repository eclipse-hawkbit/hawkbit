/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository.model;

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