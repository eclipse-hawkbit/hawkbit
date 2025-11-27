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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

/**
 * A spring Cache Manager that handles the multi tenancy.
 * <ul>
 *     <li>If a tenant is resolved by the {@link AccessContext}, a dedicated cache manager for that tenant is used/created.</li>
 *     <li>If no tenant is resolved, a global cache manager is used.</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // singleton holder ensures static access to spring resources in some places
public class TenantAwareCacheManager implements CacheManager {

    private static final String CONFIG_PREFIX = "hawkbit.cache.";
    private static final String CONFIG_SPEC = "spec";

    private static final TenantAwareCacheManager SINGLETON = new TenantAwareCacheManager();
    private CacheManager globalCacheManager;
    private final Map<String, CacheManager> tenant2CacheManager = new ConcurrentHashMap<>();

    private Environment env;

    // default caffeine cache spec - see com.github.benmanes.caffeine.cache.CaffeineSpec javadoc for format details
    private String defaultSpec;

    public static TenantAwareCacheManager getInstance() {
        return SINGLETON;
    }

    @Autowired
    public void init(final Environment env) {
        this.env = env;
        defaultSpec = env.resolvePlaceholders("${" + CONFIG_PREFIX + CONFIG_SPEC + ":expireAfterWrite=${hawkbit.cache.ttl:10s}}");
        globalCacheManager = new TenantCacheManager(null);
    }

    @NonNull
    @Override
    public Cache getCache(@NonNull final String name) {
        final Cache cache = Optional.ofNullable(AccessContext.tenant())
                .map(currentTenant -> tenant2CacheManager.computeIfAbsent(currentTenant, TenantCacheManager::new))
                .orElse(globalCacheManager)
                .getCache(name);
        return cache == null ? new Nop(name) : cache;
    }

    @NonNull
    @Override
    public Collection<String> getCacheNames() {
        final String currentTenant = AccessContext.tenant();
        if (currentTenant == null) {
            return globalCacheManager.getCacheNames();
        } else {
            final CacheManager cacheManager = tenant2CacheManager.get(currentTenant);
            return cacheManager == null ? List.of() : cacheManager.getCacheNames();
        }
    }

    /**
     * Ensures that cache eviction takes place in microservice mode in case of deletions.
     *
     * @param event The event indicating that a configuration value has been deleted.
     */
    @EventListener
    @SuppressWarnings("java:S3776") // not too complex and this way is more readable
    public void onCacheEvictEvent(final CacheEvictEvent event) {
        final CacheManager cacheManager = event.getTenant() == null ? globalCacheManager : tenant2CacheManager.get(event.getTenant());
        if (cacheManager != null) {
            if (event.getCacheName() == null) { // evict all caches
                if (event.getTenant() == null) { // global cache
                    cacheManager.getCacheNames().forEach(name -> {
                        final Cache cache = cacheManager.getCache(name);
                        if (cache != null) {
                            cache.clear();
                        }
                    });
                } else { // tenant specific cache
                    tenant2CacheManager.remove(event.getTenant());
                }
            } else {
                final Cache cache = cacheManager.getCache(event.getCacheName());
                if (cache != null) {
                    if (event.getCacheKey() == null) { // evict all keys
                        cache.clear();
                    } else { // evict specific key
                        cache.evict(event.getCacheKey());
                    }
                }
            }
        }
    }

    public interface CacheEvictEvent {

        String getTenant();

        String getCacheName(); // null means - all caches shall be evicted

        Object getCacheKey(); // null means - all keys shall be evicted

        @Value
        class Default implements CacheEvictEvent {

            String tenant;
            String cacheName;
            Object cacheKey;
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
                try {
                    return new CaffeineCache(n, Caffeine.from(spec).build(), false) {

                        @Nullable
                        @Override
                        @SuppressWarnings("java:S2638") // used internally in hawkbit and want to return null instead of error
                        protected Object toStoreValue(@Nullable Object userValue) {
                            // we want to return pure null to caffeine when null, in order to do not cache
                            // we want to allow null results but not to be cached!
                            return userValue;
                        }
                    };
                } catch (final IllegalArgumentException e) {
                    log.error("Invalid cache spec: {}", spec, e);
                    throw new IllegalStateException("Invalid cache spec: " + spec, e);
                }
            });
        }

        @NonNull
        @Override
        public Collection<String> getCacheNames() {
            return caches.keySet();
        }
    }

    @Value
    private static class Nop implements Cache {

        String name;

        @NonNull
        @Override
        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(@NonNull  final Object key) {
            return null;
        }

        @Override
        public <T> T get(@NonNull final Object key, final Class<T> type) {
            return null;
        }

        @Override
        public <T> T get(@NonNull final Object key, @NonNull final Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(@NonNull final Object key, final Object value) {
            // nop
        }

        @Override
        public void evict(@NonNull final Object key) {
            // nop
        }

        @Override
        public void clear() {
            // nop
        }
    }
}