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

public class CacheClearedEvent extends TenantAwareDistributedEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final String cacheName;

    /**
     * Constructor.
     *
     * @param tenant
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache which was cleared
     * @param applicationId
     *            the applicationId
     */
    @JsonCreator
    public CacheClearedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("cacheName") final String cacheName,
            @JsonProperty("originService") final String applicationId) {
        super(tenant, cacheName, applicationId);
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }
}
