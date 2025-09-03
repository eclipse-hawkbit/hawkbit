/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.model;

import java.io.IOException;
import java.io.InputStream;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ArtifactStream extends InputStream {

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final InputStream inputStream;
    @Getter
    private final long size;
    @Getter
    private final String sha1Hash;

    public ArtifactStream(final InputStream inputStream, final long size, final String sha1Hash) {
        this.inputStream = inputStream;
        this.size = size;
        this.sha1Hash = sha1Hash;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}