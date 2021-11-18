/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository.model;

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
