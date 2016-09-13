/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event;

/**
 * Event that contains an updated download progress for a given ActionStatus
 * that was written for a download request.
 *
 */
public class DownloadProgressEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;

    private final Long statusId;
    private final long requestedBytes;
    private final long shippedBytesSinceLast;
    private final long shippedBytesOverall;

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param statusId
     *            of ActionStatus that was written for the download request
     * @param requestedBytes
     *            bytes requested
     * @param shippedBytesSinceLast
     *            bytes since last event
     * @param shippedBytesOverall
     *            on the download request
     */
    public DownloadProgressEvent(final String tenant, final Long statusId, final Long requestedBytes,
            final Long shippedBytesSinceLast, final Long shippedBytesOverall) {
        // the revision of the DownloadProgressEvent is just equal the
        // shippedBytesOverall as this is a growing number.
        super(shippedBytesOverall, tenant);
        this.statusId = statusId;
        this.requestedBytes = requestedBytes;
        this.shippedBytesSinceLast = shippedBytesSinceLast;
        this.shippedBytesOverall = shippedBytesOverall;
    }

    public Long getStatusId() {
        return statusId;
    }

    public long getRequestedBytes() {
        return requestedBytes;
    }

    public long getShippedBytesSinceLast() {
        return shippedBytesSinceLast;
    }

    public long getShippedBytesOverall() {
        return shippedBytesOverall;
    }

    @Override
    public String getTenant() {
        // TODO Auto-generated method stub
        return null;
    }
}
