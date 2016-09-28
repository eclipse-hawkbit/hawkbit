/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.app.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * The spring Redis configuration to use a Redis server as cache.
 *
 */
@Configuration
public class RedisConfiguration {

    @Autowired
    @Lazy
    private RedisTemplate<?, ?> redisTemplate;

    /**
     * 
     * @return bean for the direct cache manager.
     */
    @Bean(name = "directCacheManager")
    public CacheManager directCacheManager() {
        return new RedisCacheManager(redisTemplate);
    }

}
