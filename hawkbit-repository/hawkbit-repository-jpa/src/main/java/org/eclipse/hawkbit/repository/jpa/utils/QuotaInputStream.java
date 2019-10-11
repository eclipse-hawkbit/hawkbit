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
import java.text.DecimalFormat;

import org.eclipse.hawkbit.repository.exception.QuotaExceededException;

public class QuotaInputStream extends FilterInputStream {

    private static final String KB = "KB";
    private static final String MB = "MB";

    private static final String MAX_ARTIFACT_SIZE_EXCEEDED = "Maximum artifact size (%s) exceeded.";
    private static final String MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED = "Storage quota exceeded, %s left.";

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
            throw new QuotaExceededException(createErrorMessage(), quotaType);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        final int read = super.read(b);

        if (size + read > quota) {
            throw new QuotaExceededException(createErrorMessage(), quotaType);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = super.read(b, off, len);

        if (size + read > quota) {
            throw new QuotaExceededException(createErrorMessage(), quotaType);
        } else if (read >= 0) {
            size += read;
        }

        return read;
    }

    private String createErrorMessage() {
        if (quotaType == QuotaExceededException.QuotaType.STORAGE_QUOTA) {
            return String.format(MAX_ARTIFACT_SIZE_TOTAL_EXCEEDED, byteValueToReadableString(quota));
        } else {
            return String.format(MAX_ARTIFACT_SIZE_EXCEEDED, byteValueToReadableString(quota));
        }
    }

    /**
     * Convert byte values to human readable strings with units
     */
    private static String byteValueToReadableString(long byteValue) {
        double outputValue = byteValue / 1024.0;
        String unit = KB;
        if (outputValue >= 1024) {
            outputValue = outputValue / 1024.0;
            unit = MB;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return new StringBuilder(df.format(outputValue)).append(" ").append(unit).toString();
    }
}
