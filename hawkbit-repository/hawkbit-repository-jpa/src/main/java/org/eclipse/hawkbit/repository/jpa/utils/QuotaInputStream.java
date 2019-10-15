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

import org.eclipse.hawkbit.repository.exception.QuotaExceededException;

public class QuotaInputStream extends FilterInputStream {

    private final long quota;

    private final QuotaExceededException.QuotaType quotaType;

    private long size;

    /**
     * Creates a <code>QuotaInputStream</code> using the input stream in and a
     * limiting quota
     * 
     * @param sizeLimit
     *            Quota file size limit in byte
     * @param storageLeft
     *            Storage left until quota is reached
     */
    public QuotaInputStream(final InputStream in, final long sizeLimit, final long storageLeft) {
        super(in);

        // only limit to lower bound and set appropriate error type
        this.quota = Math.min(sizeLimit, storageLeft);
        if (quota == sizeLimit) {
            quotaType = QuotaExceededException.QuotaType.SIZE_QUOTA;
        } else {
            quotaType = QuotaExceededException.QuotaType.STORAGE_QUOTA;
        }
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();

        if (size + read > quota) {
            throw new QuotaExceededException(quotaType, quota);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        final int read = super.read(b);

        if (size + read > quota) {
            throw new QuotaExceededException(quotaType, quota);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = super.read(b, off, len);

        if (size + read > quota) {
            throw new QuotaExceededException(quotaType, quota);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }
}
