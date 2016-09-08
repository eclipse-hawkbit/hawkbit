/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import org.eclipse.hawkbit.cache.eventbus.EventDistributor;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The spring Redis configuration which is enabled by using the profile
 * {@code redis} to use a Redis server as cache.
 *
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableScheduling
public class RedisConfiguration {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private TenantAware tenantAware;

    /**
     * @return the {@link EventDistributor} to distribute and consume the events
     *         from Redis
     */
    @Bean
    public EventDistributor eventDistributor() {
        return new EventDistributor();
    }

    /**
     * @return the spring redis cache manager.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new TenantAwareCacheManager(directCacheManager(), tenantAware);
    }

    /**
     * 
     * @return bean for the direct cache manager.
     */
    @Bean(name = "directCacheManager")
    public CacheManager directCacheManager() {
        final CompositeCacheManager compositeCacheManager = new CompositeCacheManager(redisConnectionFailoverCache());
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;
    }

    /**
     * 
     * @return bean for the direct cache manager.
     */
    @Bean
    public RedisFailoverConnectionCacheManager redisConnectionFailoverCache() {
        return new RedisFailoverConnectionCacheManager(redisTemplate());
    }

    /**
     * @return the redis connection factory to create a redis connection based
     *         on the {@link RedisProperties}
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory() {
        final JedisConnectionFactory factory = new JedisConnectionFactory();
        factory.setHostName(redisProperties.getHost());
        factory.setPort(redisProperties.getPort());
        factory.setUsePool(true);
        return factory;
    }

    /**
     * @return the spring {@link RedisTemplate} configured with the necessary
     *         object serializers
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate() {
        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        return redisTemplate;
    }

    /**
     * @return the spring-redis message listener adapter to consume messages
     *         from the Redis server
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        final MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(eventDistributor());
        messageListenerAdapter.setSerializer(new JdkSerializationRedisSerializer());
        return messageListenerAdapter;
    }

    /**
     * @return the spring-redis message listener container to register the
     *         message listener adapter
     */
    @Bean
    public RedisMessageListenerContainer redisContainer() {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter(), eventDistributor().getTopics());
        return container;
    }

}
