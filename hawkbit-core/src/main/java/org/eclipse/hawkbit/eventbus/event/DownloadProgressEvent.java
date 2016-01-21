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

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param statusId
     *            of {@link UpdateActionStatus}
     * @param progressPercent
     *            number (1-100)
     */
    public DownloadProgressEvent(final String tenant, final Long statusId, final int progressPercent) {
        // the revision of the DownloadProgressEvent is just equal the
        // progressPercentage due the
        // percentage is going from 0 to 100.
        super(statusId, tenant);
        this.statusId = statusId;
        this.progressPercent = progressPercent;
    }

    /**
     * @return the statusId
     */
    public Long getStatusId() {
        return statusId;
    }

    /**
     * @return the progressPercent
     */
    public int getProgressPercent() {
        return progressPercent;
    }
}
