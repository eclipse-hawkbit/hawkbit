/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import java.util.Collection;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class ClusterableTenantAwareCacheManager implements TenancyCacheManager {

    private final TenantAware tenantAware;
    private final TenancyCacheManager delegate;

    /**
     * Constructor.
     *
     * @param delegate
     *            the {@link CacheManager} to delegate to.
     * @param tenantAware
     *            the tenant aware to retrieve the current tenant
     */
    public ClusterableTenantAwareCacheManager(final TenancyCacheManager delegate, final TenantAware tenantAware) {
        this.delegate = delegate;
        this.tenantAware = tenantAware;
    }

    @Override
    public Cache getCache(final String name) {
        return new ClusterAwareCacheDecorator(tenantAware, delegate.getCache(name));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    @Override
    public Cache getDirectCache(final String name) {
        return delegate.getDirectCache(name);
    }

    @Override
    public void evictCaches(final String tenant) {
        delegate.evictCaches(tenant);
    }
}
