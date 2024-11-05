/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * TenantAwareEvent that contains an updated download progress for a given
 * ActionStatus that was written for a download request.
 */
public class DownloadProgressEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;

    private long shippedBytesSinceLast;

    /**
     * Default constructor.
     */
    public DownloadProgressEvent() {
        // for serialization libs like jackson
    }

    /**
     * Constructor.
     *
     * @param tenant the tenant
     * @param actionStatusId of the {@link ActionStatus} the download belongs to
     * @param shippedBytesSinceLast the shippedBytesSinceLast
     * @param applicationId the application id.
     */
    public DownloadProgressEvent(final String tenant, final Long actionStatusId, final long shippedBytesSinceLast,
            final String applicationId) {
        super(actionStatusId, tenant, applicationId);
        this.shippedBytesSinceLast = shippedBytesSinceLast;
    }

    public long getShippedBytesSinceLast() {
        return shippedBytesSinceLast;
    }

}
