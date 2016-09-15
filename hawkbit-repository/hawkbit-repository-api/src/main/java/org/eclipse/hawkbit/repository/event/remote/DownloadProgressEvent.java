/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

/**
 * Event that contains an updated download progress for a given ActionStatus
 * that was written for a download request.
 *
 */
public class DownloadProgressEvent extends TenantAwareDistributedEvent {

    private static final long serialVersionUID = 1L;

    private Long statusId;
    private long requestedBytes;
    private long shippedBytesSinceLast;
    private long shippedBytesOverall;

    /**
     * TODO Build Json Object Constructor. Add orfing Service
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
        super(shippedBytesOverall, tenant, "TODO NODEID");
        this.statusId = statusId;
        this.requestedBytes = requestedBytes;
        this.shippedBytesSinceLast = shippedBytesSinceLast;
        this.shippedBytesOverall = shippedBytesOverall;
    }

    /**
     * TODO remove just fu compilation
     * 
     * @param knownTenant
     * @param l
     * @param m
     * @param n
     * @param o
     * @param myNodeId
     */
    public DownloadProgressEvent(final String knownTenant, final long l, final long m, final long n, final long o,
            final String myNodeId) {
        super(l, knownTenant, "TODO NODEID");
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
