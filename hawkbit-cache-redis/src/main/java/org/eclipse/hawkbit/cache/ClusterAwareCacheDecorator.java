/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.eclipse.hawkbit.eventbus.event.CacheClearedEvent;
import org.eclipse.hawkbit.eventbus.event.CachedEntityEvictedEvent;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.serializer.SerializationException;

class ClusterAwareCacheDecorator implements Cache {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareCacheDecorator.class);

    private final TenantAware tenantAware;
    private final Cache delegate;

    public ClusterAwareCacheDecorator(final TenantAware tenantAware, final Cache cache) {
        this.tenantAware = checkNotNull(tenantAware);
        this.delegate = checkNotNull(cache);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(final Object key) {

        try {
            return delegate.get(key);
        } catch (final SerializationException ex) {
            LOG.warn("Failed to restore cached value for key {}", key, ex);
            return null; // simulate no cache hit
        }
    }

    @Override
    public <T> T get(final Object key, final Class<T> type) {
        try {
            return delegate.get(key, type);
        } catch (final SerializationException ex) {
            LOG.warn("Failed to restore cached value for key {}", key, ex);
            return null; // simulate no cache hit
        }
    }

    @Override
    public void put(final Object key, final Object value) {
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(final Object key, final Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(final Object key) {

        final String tenant = tenantAware.getCurrentTenant();

        delegate.evict(key);

        if (tenant != null) {
            // TODO_DSC: send final event to other final cluster nodes.
            // Clarify => Serializable !
            new CachedEntityEvictedEvent(tenant, delegate.getName(), (Serializable) key);
        }
    }

    @Override
    public void clear() {

        final String tenant = tenantAware.getCurrentTenant();

        delegate.clear();

        if (tenant != null) {
            // TODO_DSC: send final event to other final cluster nodes
            new CacheClearedEvent(tenant, delegate.getName());
        }
    }
}
