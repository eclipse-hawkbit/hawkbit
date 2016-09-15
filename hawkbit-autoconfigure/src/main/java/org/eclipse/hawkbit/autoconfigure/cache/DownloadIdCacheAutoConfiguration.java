/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.cache;

import org.eclipse.hawkbit.cache.DefaultDownloadIdCache;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration for configuring a cache for the download id's.
 *
 * This is done by providing a named cache.
 */
@Configuration
public class DownloadIdCacheAutoConfiguration {

    @Autowired
    private CacheManager cacheManager;

    /**
     * Bean for the download id cache.
     * 
     * @return the cache
     */
    @Bean
    @ConditionalOnMissingBean
    public DownloadIdCache downloadIdCache() {
        return new DefaultDownloadIdCache(cacheManager);
    }

}
