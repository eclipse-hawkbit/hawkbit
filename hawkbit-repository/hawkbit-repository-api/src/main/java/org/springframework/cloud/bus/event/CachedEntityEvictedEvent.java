/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.springframework.cloud.bus.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

public class CachedEntityEvictedEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;

    private static final int NO_REVISION = -1;

    private final String cacheName;
    private final Serializable cacheKey;

    /**
     * Constructor.
     *
     * @param tanent
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache where on which the eviction appeared
     * @param cacheKey
     *            the cached key from the entity which has changed
     */
    public CachedEntityEvictedEvent(final String tanent, final String cacheName, final Serializable cacheKey) {
        super(NO_REVISION, tanent);
        this.cacheName = checkNotNull(cacheName);
        this.cacheKey = checkNotNull(cacheKey);
    }

    public String getCacheName() {
        return cacheName;
    }

    public Serializable getCacheKey() {
        return cacheKey;
    }

    @Override
    public String getTenant() {
        // TODO Auto-generated method stub
        return null;
    }
}