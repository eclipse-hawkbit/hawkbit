/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.client.scenarios.upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation for {@link MultipartFile} for hawkBit artifact upload.
 *
 */
public class ArtifactFile implements MultipartFile {

    private final String name;

    private final String originalFilename;

    private final String contentType;

    private final byte[] content;

    /**
     * Create a new ArtifactFile with the given content.
     * 
     * @param name
     *            the name of the file
     * @param content
     *            the content of the file
     */
    public ArtifactFile(final String name, final byte[] content) {
        this(name, "", null, content);
    }

    /**
     * Create a new ArtifactFile with the given content.
     * 
     * @param name
     *            of the file
     * @param originalFilename
     *            the original filename (as on the client's machine)
     * @param contentType
     *            the content type
     * @param content
     *            of the file
     */
    public ArtifactFile(final String name, final String originalFilename, final String contentType,
            final byte[] content) {
        Assert.hasLength(name, "Name must not be null");
        this.name = name;
        this.originalFilename = Optional.ofNullable(originalFilename).orElse("");
        this.contentType = contentType;
        this.content = Optional.ofNullable(content).orElse(new byte[0]);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return this.content.length == 0;
    }

    @Override
    public long getSize() {
        return this.content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return this.content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public void transferTo(final File dest) throws IOException {
        FileCopyUtils.copy(this.content, dest);
    }

}
