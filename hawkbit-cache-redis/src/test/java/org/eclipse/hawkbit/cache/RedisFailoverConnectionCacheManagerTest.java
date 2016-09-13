/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 * 
 */
@Features("Unit Tests - Cluster Cache")
@Stories("Redis failover connection manager Test")
@RunWith(MockitoJUnitRunner.class)
public class RedisFailoverConnectionCacheManagerTest {

    private static String CACHE_NAME = "testCache";

    private RedisFailoverConnectionCacheManager redisFailoverConnectionCacheManagerUnderTest;

    @Mock
    private RedisTemplate<?, ?> redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private JedisConnection jedisConnection;

    @Before
    public void setupTest() {
        when(connectionFactory.getConnection()).thenReturn(jedisConnection);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        redisFailoverConnectionCacheManagerUnderTest = new RedisFailoverConnectionCacheManager(redisTemplate);
    }

    @Test
    @Description("Verify that the cache manager return a cache when a redis connection is available")
    public void testGetCacheWithConnection() {
        redisFailoverConnectionCacheManagerUnderTest.pingRedis();
        final Cache cache = redisFailoverConnectionCacheManagerUnderTest.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
    }

    @Test
    @Description("Verify that the cache manager return a null when a redis connection is no reachable")
    public void testGetCacheWithOutConnection() {
        redisFailoverConnectionCacheManagerUnderTest.pingRedis();
        when(connectionFactory.getConnection()).thenThrow(new RedisConnectionFailureException(""));
        Cache cache = redisFailoverConnectionCacheManagerUnderTest.getCache(CACHE_NAME);
        assertThat(cache).isNull();

        // ping will not work
        redisFailoverConnectionCacheManagerUnderTest.pingRedis();
        cache = redisFailoverConnectionCacheManagerUnderTest.getCache(CACHE_NAME);
        assertThat(cache).isNull();

    }

}
