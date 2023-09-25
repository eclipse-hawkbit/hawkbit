/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

/**
 * Listener for progress on artifact file streaming.
 *
 */
@FunctionalInterface
public interface FileStreamingProgressListener {

    /**
     * Called multiple times during streaming.
     * 
     * @param requestedBytes
     *            requested bytes of the request
     * @param shippedBytesSinceLast
     *            since the last report
     * @param shippedBytesOverall
     *            during the request
     */
    void progress(long requestedBytes, long shippedBytesSinceLast, long shippedBytesOverall);
}
