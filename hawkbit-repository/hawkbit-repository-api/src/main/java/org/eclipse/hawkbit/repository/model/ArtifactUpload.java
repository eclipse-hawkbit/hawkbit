/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.InputStream;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.ValidString;

/**
 * Use to create a new artifact.
 *
 */
public class ArtifactUpload {

    @NotNull
    private final InputStream inputStream;

    private final long moduleId;

    @NotEmpty
    @ValidString
    private final String filename;

    private final String providedMd5Sum;

    private final String providedSha1Sum;

    private final String providedSha256Sum;

    private final boolean overrideExisting;

    private final String contentType;

    private final long filesize;

    /**
     * Constructor
     * 
     * @param inputStream
     *            to read from for artifact binary
     * @param moduleId
     *            to assign the new artifact to
     * @param filename
     *            of the artifact
     * @param overrideExisting
     *            to <code>true</code> if the artifact binary can be overridden
     *            if it already exists
     * @param filesize
     *            the size of the file in bytes.
     */
    public ArtifactUpload(final InputStream inputStream, final long moduleId, final String filename,
            final boolean overrideExisting, final long filesize) {
        this(inputStream, moduleId, filename, null, null, null, overrideExisting, null, filesize);
    }

    /**
     * Constructor
     * 
     * @param inputStream
     *            to read from for artifact binary
     * @param moduleId
     *            to assign the new artifact to
     * @param filename
     *            of the artifact
     * @param providedSha1Sum
     *            optional sha1 checksum to check the new file against
     * @param providedMd5Sum
     *            optional md5 checksum to check the new file against
     * @param overrideExisting
     *            to <code>true</code> if the artifact binary can be overridden
     *            if it already exists
     * @param contentType
     *            the contentType of the file
     * @param filesize
     *            the size of the file in bytes.
     */
    public ArtifactUpload(final InputStream inputStream, final long moduleId, final String filename,
            final String providedMd5Sum, final String providedSha1Sum, final String providedSha256Sum,
            final boolean overrideExisting, final String contentType, final long filesize) {
        this.inputStream = inputStream;
        this.moduleId = moduleId;
        this.filename = filename;
        this.providedMd5Sum = providedMd5Sum;
        this.providedSha1Sum = providedSha1Sum;
        this.providedSha256Sum = providedSha256Sum;
        this.overrideExisting = overrideExisting;
        this.contentType = contentType;
        this.filesize = filesize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getModuleId() {
        return moduleId;
    }

    public String getFilename() {
        return filename;
    }

    public String getProvidedMd5Sum() {
        return providedMd5Sum;
    }

    public String getProvidedSha1Sum() {
        return providedSha1Sum;
    }

    public String getProvidedSha256Sum() {
        return providedSha256Sum;
    }

    public boolean overrideExisting() {
        return overrideExisting;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFilesize() {
        return filesize;
    }
}
