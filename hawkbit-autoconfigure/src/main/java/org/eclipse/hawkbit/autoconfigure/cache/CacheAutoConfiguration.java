/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

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

    /**
     * @return the default cache manager bean if none other cache manager is
     *         existing.
     */
    @Bean
    @ConditionalOnMissingBean
    @Primary
    TenancyCacheManager cacheManager(@Qualifier("directCacheManager") final CacheManager directCacheManager,
            final TenantAware tenantAware) {
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

        /**
         * @return the direct cache manager to access without tenant aware
         *         check, cause in sometimes it's necessary to access the cache
         *         directly without having the current tenant, e.g. initial
         *         creation of tenant
         */
        @Bean(name = "directCacheManager")
        @ConditionalOnMissingBean(name = "directCacheManager")
        public CacheManager directCacheManager(final CacheProperties cacheProperties) {
            final CaffeineCacheManager cacheManager = new CaffeineCacheManager();

            if (cacheProperties.getTtl() > 0) {
                final Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.getTtl(), cacheProperties.getTtlUnit());
                cacheManager.setCaffeine(cacheBuilder);
            }

            return cacheManager;
        }

    }
}
