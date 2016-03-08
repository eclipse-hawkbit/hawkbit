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
import java.util.Collections;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A {@link CacheManager} delegator which wraps the
 * {@link CacheManager#getCache(String)} and
 * {@link CacheManager#getCacheNames()} to include the
 * {@link TenantAware#getCurrentTenant()} when accessing a cache, so caches are
 * seperated.
 *
 * Additionally it also provide functionality to retrieve all caches overall
 * tenants at once, for monitoring and system access.
 *
 *
 *
 *
 */
public class TenantAwareCacheManager implements TenancyCacheManager {

    private static final String TENANT_CACHE_DELIMITER = "|";

    private final CacheManager delegate;

    private final TenantAware tenantAware;

    /**
     * @param delegate
     *            the {@link CacheManager} to delegate to.
     * @param tenantAware
     *            the tenant aware to retrieve the current tenant
     */
    public TenantAwareCacheManager(final CacheManager delegate, final TenantAware tenantAware) {
        this.delegate = delegate;
        this.tenantAware = tenantAware;
    }

    @Override
    public Cache getCache(final String name) {
        String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant == null) {
            return null;
        }

        currentTenant = currentTenant.toUpperCase();
        if (currentTenant.contains(TENANT_CACHE_DELIMITER)) {
            return null;
        }
        return delegate.getCache(currentTenant + TENANT_CACHE_DELIMITER + name);
    }

    @Override
    public Collection<String> getCacheNames() {
        String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant == null) {
            return null;
        }

        currentTenant = currentTenant.toUpperCase();
        if (currentTenant.contains(TENANT_CACHE_DELIMITER)) {
            return Collections.emptyList();
        }
        return getCacheNames(currentTenant);
    }

    /**
     * A direct access for retrieving all cache names overall tenants.
     * 
     * @return all cache names without tenant check
     */
    public Collection<String> getDirectCacheNames() {
        return delegate.getCacheNames();
    }

    @Override
    public Cache getDirectCache(final String name) {
        return delegate.getCache(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.cache.TenancyCacheManager#evictCaches(java.
     * lang. String)
     */
    @Override
    public void evictCaches(final String tenant) {
        getCacheNames(tenant)
                .forEach(cachename -> delegate.getCache(tenant + TENANT_CACHE_DELIMITER + cachename).clear());
    }

    private Collection<String> getCacheNames(final String tenant) {
        final String tenantWithDelimiter = tenant + TENANT_CACHE_DELIMITER;
        return delegate.getCacheNames().parallelStream().filter(cacheName -> cacheName.startsWith(tenantWithDelimiter))
                .map(cacheName -> cacheName.substring(tenantWithDelimiter.length())).collect(Collectors.toList());
    }
}
