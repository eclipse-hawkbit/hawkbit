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
import java.io.OutputStream;

/**
 * Database representation of artifact.
 * 
 *
 *
 *
 */
public class DbArtifact {

    private String artifactId;

    private DbArtifactHash hashes;

    private Long size;

    private String contentType;

    private InputStream fileInputStream;

    private OutputStream fileOutputStream;

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public DbArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DbArtifactHash hashes) {
        this.hashes = hashes;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setFileInputStream(final InputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    public OutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void setFileOutputStream(final OutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }
}
