/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import lombok.NonNull;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * A {@link CacheManager} delegator which wraps the {@link CacheManager#getCache(String)} and {@link CacheManager#getCacheNames()}
 * to include the {@link TenantAware#getCurrentTenant()} when accessing a cache, so caches are seperated.
 * <p/>
 * Additionally, it also provides functionality to retrieve all caches overall tenants at once, for monitoring and system access.
 */
public class TenantAwareCacheManager implements TenancyCacheManager {

    private static final String TENANT_CACHE_DELIMITER = "|";

    private final CacheManager delegate;
    private final TenantAware tenantAware;

    /**
     * Constructor.
     *
     * @param delegate the {@link CacheManager} to delegate to.
     * @param tenantAware the tenant aware to retrieve the current tenant
     */
    public TenantAwareCacheManager(final CacheManager delegate, final TenantAware tenantAware) {
        this.delegate = delegate;
        this.tenantAware = tenantAware;
    }

    @Nullable
    @Override
    public Cache getCache(@NonNull final String name) {
        final String currentTenant = tenantAware.getCurrentTenant();
        if (isTenantInvalid(currentTenant)) {
            return null;
        }

        return delegate.getCache(buildKey(currentTenant.toUpperCase(), name));
    }

    @NonNull
    @Override
    public Collection<String> getCacheNames() {
        final String currentTenant = tenantAware.getCurrentTenant();
        if (isTenantInvalid(currentTenant)) {
            return Collections.emptyList();
        }

        return getCacheNames(currentTenant.toUpperCase());
    }

    @Override
    public Cache getDirectCache(final String name) {
        return delegate.getCache(name);
    }

    @Override
    public void evictCaches(final String tenant) {
        getCacheNames(tenant).forEach(
                cacheName -> Optional.ofNullable(delegate.getCache(buildKey(tenant, cacheName))).ifPresent(Cache::clear));
    }

    /**
     * A direct-access for retrieving all cache names overall tenants.
     *
     * @return all cache names without tenant check
     */
    public Collection<String> getDirectCacheNames() {
        return delegate.getCacheNames();
    }

    private static boolean isTenantInvalid(final String tenant) {
        return tenant == null || tenant.contains(TENANT_CACHE_DELIMITER);
    }

    private static String buildKey(final String tenant, final String cacheName) {
        return tenant + TENANT_CACHE_DELIMITER + cacheName;
    }

    private Collection<String> getCacheNames(final String tenant) {
        final String tenantWithDelimiter = tenant + TENANT_CACHE_DELIMITER;
        return delegate.getCacheNames().parallelStream()
                .filter(cacheName -> cacheName.startsWith(tenantWithDelimiter))
                .map(cacheName -> cacheName.substring(tenantWithDelimiter.length()))
                .toList();
    }
}