/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.google.common.cache.CacheBuilder;

/**
 * A configuration for configuring the spring {@link CacheManager} for specific
 * multi-tenancy caching. The caches between tenants must not interfere each
 * other.
 *
 * This is done by providing a special {@link TenantCacheResolver} which
 * generates a cache name included the current tenant.
 */
@Configuration
@EnableCaching
public class CacheAutoConfiguration {

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    @Qualifier("directCacheManager")
    private CacheManager directCacheManager;

    /**
     * @return the default cache manager bean if none other cache manager is
     *         existing.
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    public TenancyCacheManager cacheManager() {
        return new TenantAwareCacheManager(directCacheManager, tenantAware);
    }

    /**
     * A separate configuration of the direct cache manager for the
     * {@link TenantAwareCacheManager} that it can get overridden by another
     * configuration.
     */
    @Configuration
    @EnableConfigurationProperties(CacheProperties.class)
    static class DirectCacheManagerConfiguration {

        @Autowired
        private CacheProperties cacheProperties;

        /**
         * @return the direct cache manager to access without tenant aware
         *         check, cause in sometimes it's necessary to access the cache
         *         directly without having the current tenant, e.g. initial
         *         creation of tenant
         */
        @Bean(name = "directCacheManager")
        @ConditionalOnMissingBean(name = "directCacheManager")
        public CacheManager directCacheManager() {
            final GuavaCacheManager cacheManager = new GuavaCacheManager();

            if (cacheProperties.getTtl() > 0) {
                final CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                        .expireAfterWrite(cacheProperties.getTtl(), cacheProperties.getTtlUnit());
                cacheManager.setCacheBuilder(cacheBuilder);
            }

            return cacheManager;
        }

    }

    /**
     * A {@link SimpleCacheResolver} implementation which includes the
     * {@link TenantAware#getCurrentTenant()} into the cache name before
     * resolving it.
     */
    public class TenantCacheResolver extends SimpleCacheResolver {

        @Override
        public Collection<Cache> resolveCaches(final CacheOperationInvocationContext<?> context) {
            return super.resolveCaches(context).stream().map(TenantCacheWrapper::new).collect(Collectors.toList());
        }

        @Override
        protected Collection<String> getCacheNames(final CacheOperationInvocationContext<?> context) {
            return super.getCacheNames(context).stream()
                    .map(cacheName -> tenantAware.getCurrentTenant() + "." + cacheName).collect(Collectors.toList());
        }
    }

    /**
     * An {@link Cache} wrapper which returns the name of the cache include the
     * {@link TenantAware#getCurrentTenant()}.
     *
     *
     *
     *
     */
    public class TenantCacheWrapper implements Cache {
        private final Cache delegate;

        /**
         * @param delegate
         */
        public TenantCacheWrapper(final Cache delegate) {
            this.delegate = delegate;
        }

        /**
         * @return
         * @see org.springframework.cache.Cache#getName()
         */
        @Override
        public String getName() {
            return tenantAware.getCurrentTenant() + "." + delegate.getName();
        }

        /**
         * @return
         * @see org.springframework.cache.Cache#getNativeCache()
         */
        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        /**
         * @param key
         * @return
         * @see org.springframework.cache.Cache#get(java.lang.Object)
         */
        @Override
        public ValueWrapper get(final Object key) {
            return delegate.get(key);
        }

        /**
         * @param key
         * @param type
         * @return
         * @see org.springframework.cache.Cache#get(java.lang.Object,
         *      java.lang.Class)
         */
        @Override
        public <T> T get(final Object key, final Class<T> type) {
            return delegate.get(key, type);
        }

        /**
         * @param key
         * @param value
         * @see org.springframework.cache.Cache#put(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void put(final Object key, final Object value) {
            delegate.put(key, value);
        }

        /**
         * @param key
         * @param value
         * @return
         * @see org.springframework.cache.Cache#putIfAbsent(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public ValueWrapper putIfAbsent(final Object key, final Object value) {
            return delegate.putIfAbsent(key, value);
        }

        /**
         * @param key
         * @see org.springframework.cache.Cache#evict(java.lang.Object)
         */
        @Override
        public void evict(final Object key) {
            delegate.evict(key);
        }

        /**
         *
         * @see org.springframework.cache.Cache#clear()
         */
        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public <T> T get(final Object key, final Callable<T> valueLoader) {
            return delegate.get(key, valueLoader);
        }

    }

}
