/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event that contains an updated download progress for a given ActionStatus
 * that was written for a download request.
 *
 */
public class DownloadProgressEvent extends TenantAwareDistributedEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final Long shippedBytesSinceLast;

    /**
     * Constructor.
     * 
     * @param tenant
     *            the tenant
     * @param shippedBytesSinceLast
     *            the shippedBytesSinceLast
     * @param applicationId
     *            the application id.
     */
    @JsonCreator
    public DownloadProgressEvent(final @JsonProperty("tenant") String tenant,
            final @JsonProperty("shippedBytesSinceLast") Long shippedBytesSinceLast,
            final @JsonProperty("originService") String applicationId) {
        super(shippedBytesSinceLast, tenant, applicationId);
        this.shippedBytesSinceLast = shippedBytesSinceLast;
    }

    public long getShippedBytesSinceLast() {
        return shippedBytesSinceLast;
    }

}
