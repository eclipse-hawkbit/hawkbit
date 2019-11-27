/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.hawkbit.repository.exception.FileSizeQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.StorageQuotaExceededException;

/**
 * A FilterInputStream that ensures file size and storage quotas are enforced. It check during read operations if the
 * quota will be exceeded and throws an QuotaExceededException if this happens.
 */
public class FileSizeAndStorageQuotaCheckingInputStream extends FilterInputStream {

    private final long quota;
    private final long sizeLimit;

    private long size;

    /**
     * Creates a <code>QuotaInputStream</code> using the input stream in and a
     * limiting quota
     *
     * @param in
     *            Inner InputStream that read operations will be forwarded to
     * @param sizeLimit
     *            Quota file size limit in byte
     * @param storageLeft
     *            Storage left until quota is reached
     */
    public FileSizeAndStorageQuotaCheckingInputStream(final InputStream in, final long sizeLimit, final long storageLeft) {
        super(in);

        // only limit to lower bound to avoid two checks
        this.quota = Math.min(sizeLimit, storageLeft);
        this.sizeLimit = sizeLimit;
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();
        checkQuotaAndUpdateSize(read);
        return read;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        final int read = super.read(b);
        checkQuotaAndUpdateSize(read);
        return read;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = super.read(b, off, len);
        checkQuotaAndUpdateSize(read);
        return read;
    }

    private void checkQuotaAndUpdateSize(final int read) {
        if ((size + read) > quota) {
            // pick exception based on quota type
            if (quota == sizeLimit) {
                throw new FileSizeQuotaExceededException(quota);
            } else {
                throw new StorageQuotaExceededException(quota);
            }
        } else if (read >= 0) {
            size += read;
        }
    }
}
