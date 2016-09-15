/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * Defines the remote event of clear a cache event for a cache key.
 */
public class CachedEntityEvictedEvent extends TenantAwareDistributedEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty(required = true)
    private final String cacheName;

    @JsonProperty(required = true)
    private final Serializable cacheKey;

    /**
     * Constructor.
     * 
     * @param tenant
     * @param cacheName
     *            the name of the cache which was cleared
     * @param cacheKey
     *            the key of the cache entry which was cleared
     * @param applicationId
     *            the origin application id
     */
    @JsonCreator
    public CachedEntityEvictedEvent(@JsonProperty("tenant") final String tenant,
            @JsonProperty("cacheName") final String cacheName,
            @JsonProperty("cacheKey") @NotNull final Serializable cacheKey,
            @JsonProperty("originService") final String applicationId) {
        super(cacheName, tenant, applicationId);
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Serializable getCacheKey() {
        return cacheKey;
    }

}