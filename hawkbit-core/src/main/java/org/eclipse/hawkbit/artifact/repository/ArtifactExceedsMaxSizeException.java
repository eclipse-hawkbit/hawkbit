/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

/**
 * Thrown when an artifact exceeds the configured maximum size.
 */
public class ArtifactExceedsMaxSizeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_TEMPLATE = "The uploaded artifact '%s' exceeds the maximum artifact size that is allowed. "
            + "The artifact has a size of %s bytes. The configured maximum size is %s bytes.";

    /**
     * The filename of the artifact
     */
    private final String filename;

    /**
     * The maximum artifact size in bytes
     */
    private final long maxBytes;

    /**
     * The actual artifact size in bytes
     */
    private final long bytes;

    /**
     * Constructs a ArtifactExceedsMaxSizeException.
     * 
     * @param filename
     *            the name of the file
     * @param bytes
     *            the actual artifact size in bytes
     * @param maxBytes
     *            the maximum artifact size in bytes
     */
    public ArtifactExceedsMaxSizeException(final String filename, final long bytes, final long maxBytes) {
        this(String.format(MESSAGE_TEMPLATE, filename, bytes, maxBytes), filename, bytes, maxBytes);
    }

    /**
     * Constructs a ArtifactExceedsMaxSizeException with a custom message.
     * 
     * @param message
     *            the message of the exception
     * @param filename
     *            the name of the file
     * @param bytes
     *            the actual artifact size in bytes
     * @param maxBytes
     *            the maximum artifact size in bytes
     */
    public ArtifactExceedsMaxSizeException(final String message, final String filename, final long bytes,
            final long maxBytes) {
        super(message);
        this.filename = filename;
        this.bytes = bytes;
        this.maxBytes = maxBytes;
    }

    public String getFilename() {
        return filename;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public long getBytes() {
        return bytes;
    }

}
