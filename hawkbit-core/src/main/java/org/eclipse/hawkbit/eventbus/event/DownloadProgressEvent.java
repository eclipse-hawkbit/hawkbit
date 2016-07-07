/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

/**
 * Event that contains an updated download progress for a given Action.
 *
 *
 *
 *
 */
public class DownloadProgressEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;

    private final Long statusId;
    private final int progressPercent;
    private final long shippedBytesSinceLast;
    private final long shippedBytesOverall;

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param statusId
     *            of {@link ActionStatus}
     * @param progressPercent
     *            number (1-100)
     * @param shippedBytesSinceLast
     *            bytes since last event
     * @param shippedBytesOverall
     *            on the download request
     */
    public DownloadProgressEvent(final String tenant, final Long statusId, final int progressPercent,
            final long shippedBytesSinceLast, final long shippedBytesOverall) {
        // the revision of the DownloadProgressEvent is just equal the
        // progressPercentage due the
        // percentage is going from 0 to 100.
        super(statusId, tenant);
        this.statusId = statusId;
        this.progressPercent = progressPercent;
        this.shippedBytesSinceLast = shippedBytesSinceLast;
        this.shippedBytesOverall = shippedBytesOverall;
    }

    public Long getStatusId() {
        return statusId;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public long getShippedBytesSinceLast() {
        return shippedBytesSinceLast;
    }

    public long getShippedBytesOverall() {
        return shippedBytesOverall;
    }
}
