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
