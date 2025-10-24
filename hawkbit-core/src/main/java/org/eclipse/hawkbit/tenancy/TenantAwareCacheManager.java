/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.eclipse.hawkbit.tenancy.TenantAware.TenantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

/**
 * A spring Cache Manager that handles the multi tenancy.
 * <ul>
 *     <li>If a tenant is resolved by the {@link TenantResolver}, a dedicated cache manager for that tenant is used/created.</li>
 *     <li>If no tenant is resolved, a global cache manager is used.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // singleton holder ensures static access to spring resources in some places
public class TenantAwareCacheManager implements CacheManager {

    private static final String CONFIG_PREFIX = "hawkbit.cache.";
    private static final String CONFIG_SPEC = "spec";

    private static final TenantAwareCacheManager SINGLETON = new TenantAwareCacheManager();

    private CacheManager globalCacheManager;
    private final Map<String, CacheManager> tenant2CacheManager = new ConcurrentHashMap<>();

    private TenantResolver resolver;
    private Environment env;

    // default caffeine cache spec - see com.github.benmanes.caffeine.cache.CaffeineSpec javadoc for format details
    private String defaultSpec;

    public static TenantAwareCacheManager getInstance() {
        return SINGLETON;
    }

    @Autowired
    public void init(final TenantResolver resolver, final Environment env) {
        this.resolver = resolver;
        this.env = env;
        defaultSpec = env.resolvePlaceholders("${" + CONFIG_PREFIX + CONFIG_SPEC + ":expireAfterWrite=${hawkbit.cache.ttl:10s}}");
        globalCacheManager = new TenantCacheManager(null);
    }

    @Nullable
    @Override
    public Cache getCache(@NonNull final String name) {
        return Optional.ofNullable(resolver.resolveTenant())
                .map(currentTenant -> tenant2CacheManager.computeIfAbsent(currentTenant, TenantCacheManager::new))
                .orElse(globalCacheManager)
                .getCache(name);
    }

    @NonNull
    @Override
    public Collection<String> getCacheNames() {
        final String currentTenant = resolver.resolveTenant();
        if (currentTenant == null) {
            return globalCacheManager.getCacheNames();
        } else {
            final CacheManager cacheManager = tenant2CacheManager.get(currentTenant);
            return cacheManager == null ? List.of() : cacheManager.getCacheNames();
        }
    }

    public void evictTenant(final String tenant) {
        if (tenant == null) {
            globalCacheManager.getCacheNames().forEach(name -> Optional.ofNullable(globalCacheManager.getCache(name)).ifPresent(Cache::clear));
        } else {
            tenant2CacheManager.remove(tenant);
        }
    }

    private class TenantCacheManager implements CacheManager {

        private final String tenant;
        private final Map<String, Cache> caches = new ConcurrentHashMap<>();

        private TenantCacheManager(final String tenant) {
            this.tenant = tenant;
        }

        @Nullable
        @Override
        public Cache getCache(@NonNull final String name) {
            return caches.computeIfAbsent(name, n -> {
                // try tenant and cache specific config first
                String spec = env.getProperty(CONFIG_PREFIX + (tenant == null ? "" : tenant + ".") + name + "." + CONFIG_SPEC);
                if (spec == null) {
                    // try cache specific config next
                    spec = env.getProperty(CONFIG_PREFIX + name + "." + CONFIG_SPEC);
                    if (spec == null) {
                        spec = defaultSpec;
                    }
                }
                return new CaffeineCache(n, Caffeine.from(spec).build(), false);
            });
        }

        @NonNull
        @Override
        public Collection<String> getCacheNames() {
            return caches.keySet();
        }
    }
}