/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

/**
 * Byte range for resume download operations.
 *
 *
 *
 *
 *
 */
public class ByteRange {
    public static final String MULTIPART_BOUNDARY = "THIS_STRING_SEPARATES_MULTIPART";

    private final long start;
    private final long end;
    private final long length;
    private final long total;

    /**
     * Construct a byte range.
     * 
     * @param start
     *            Start of the byte range.
     * @param end
     *            End of the byte range.
     * @param total
     *            Total length of the byte source.
     */
    public ByteRange(final long start, final long end, final long total) {
        this.start = start;
        this.end = end;
        length = end - start + 1;
        this.total = total;
    }

    /**
     * @return the start
     */
    public long getStart() {
        return start;
    }

    /**
     * @return the end
     */
    public long getEnd() {
        return end;
    }

    /**
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * @return the total
     */
    public long getTotal() {
        return total;
    }

    @Override
    // NOSONAR - as this is generated
    @SuppressWarnings("squid:S864")
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (end ^ end >>> 32);
        result = prime * result + (int) (length ^ length >>> 32);
        result = prime * result + (int) (start ^ start >>> 32);
        result = prime * result + (int) (total ^ total >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) { // NOSONAR - as this is generated
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteRange other = (ByteRange) obj;
        if (end != other.end) {
            return false;
        }
        if (length != other.length) {
            return false;
        }
        if (start != other.start) {
            return false;
        }
        if (total != other.total) {
            return false;
        }
        return true;
    }

}
