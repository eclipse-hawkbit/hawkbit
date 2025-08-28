/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.artifact.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.artifact.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifactHash;

/**
 * {@link AbstractDbArtifact} implementation which dynamically creates a {@link FileInputStream} on calling {@link #getFileInputStream()}.
 */
@EqualsAndHashCode(callSuper = true)
public class ArtifactFilesystem extends AbstractDbArtifact {

    private final File file;

    public ArtifactFilesystem(
            @NotNull final File file, @NotNull final String artifactId,
            @NotNull final DbArtifactHash hashes, final Long size,
            final String contentType) {
        super(artifactId, hashes, size, contentType);
        this.file = Objects.requireNonNull(file, "Artifact file may not be null");
    }

    // suppress warning, this InputStream needs to be closed by the caller, this
    // cannot be closed in this method
    @SuppressWarnings("squid:S2095")
    @Override
    public InputStream getFileInputStream() {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (final FileNotFoundException e) {
            throw new ArtifactBinaryNotFoundException("Artifact file not found!");
        }
    }
}