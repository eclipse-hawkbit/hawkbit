/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;

/**
 * A {@link DbArtifact} implementation which dynamically creates a
 * {@link FileInputStream} on calling {@link #getFileInputStream()}.
 */
public class ArtifactFilesystem extends DbArtifact {

    private final File file;

    ArtifactFilesystem(final File file) {
        this.file = file;
    }

    @Override
    // suppress warning, this InputStream needs to be closed by the caller, this
    // cannot be closed in this method
    @SuppressWarnings("squid:S2095")
    public InputStream getFileInputStream() {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
