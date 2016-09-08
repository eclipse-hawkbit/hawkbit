/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * This customize cache manager will return a null cache if the redis connection
 * fails.
 * 
 * Currently there is no fallback strategy implemented:
 * https://jira.spring.io/browse/DATAREDIS-349
 * 
 * http://stackoverflow.com/questions/29003786/how-to-disable-redis-caching-at-
 * run-time-if-redis-connection-failed
 *
 * 
 */
public class RedisFailoverConnectionCacheManager extends RedisCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisFailoverConnectionCacheManager.class);

    private final AtomicBoolean connectionAlive = new AtomicBoolean(false);

    /**
     * Constructor.
     * 
     * @param redisTemplate
     *            the redis helper template
     */
    public RedisFailoverConnectionCacheManager(final RedisTemplate<?, ?> redisTemplate) {
        super(redisTemplate);
        pingRedis();
    }

    @Override
    public Cache getCache(final String name) {
        if (connectionAlive.get() && isConnectionAlive()) {
            return super.getCache(name);
        }
        connectionAlive.set(false);
        return null;
    }

    @Override
    protected RedisTemplate<?, ?> getRedisOperations() {
        return (RedisTemplate<?, ?>) super.getRedisOperations();
    }

    @Scheduled(fixedRate = 5000)
    protected void pingRedis() {
        connectionAlive.set(isConnectionAlive());
    }

    private boolean isConnectionAlive() {
        RedisConnection connection = null;
        final RedisConnectionFactory connectionFactory = getRedisOperations().getConnectionFactory();
        try {
            connection = RedisConnectionUtils.getConnection(connectionFactory);
            connection.ping();
            return true;
        } catch (final RedisConnectionFailureException e) {
            LOGGER.error("Redis server is unreachable", e);
            return false;
        } finally {
            RedisConnectionUtils.releaseConnection(connection, connectionFactory);
        }
    }

}
