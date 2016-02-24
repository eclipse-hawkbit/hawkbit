/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache.eventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.hawkbit.eventbus.EventSubscriber;
import org.eclipse.hawkbit.eventbus.event.DistributedEvent;
import org.eclipse.hawkbit.eventbus.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 *
 *
 */
@EventSubscriber
public class EventDistributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDistributor.class);
    /**
     * the node id to filter self published events in the redis message
     * subscriber.
     */
    private static final String NODE_ID = UUID.randomUUID().toString();

    private static final String DISTRIBUTION_CHANNEL_TOPIC = "com/bosch/sp/distEvent";
    private static final String SEND_DISTRIBUTION_CHANNEL = DISTRIBUTION_CHANNEL_TOPIC + "/" + NODE_ID;
    private static final String SUB_DISTRIBUTION_CHANNEL = DISTRIBUTION_CHANNEL_TOPIC + "*";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EventBus eventBus;

    /**
     * consumes all {@link DistributedEvent}s posted on the {@link EventBus} and
     * distribute them to the Redis server.
     * 
     * @param event
     *            the distributed event posted on the {@link EventBus}
     */
    @Subscribe
    public void distribute(final DistributedEvent event) {
        if (redisTemplate != null) {
            if (!NODE_ID.equals(event.getNodeId())) {
                logDistributingEvent(event, SEND_DISTRIBUTION_CHANNEL);
                event.setOriginNodeId(NODE_ID);
                redisTemplate.convertAndSend(SEND_DISTRIBUTION_CHANNEL, event);
            }
        } else {
            logNotDistributingEvent(event, SEND_DISTRIBUTION_CHANNEL);
        }
    }

    /**
     * message listener callback method for the {@link MessageListenerAdapter}
     * which calls the method in case a message is received from the Redis
     * server of the type {@link DistributedEventWrapper}.
     * 
     * @param event
     *            the {@link DistributedEventWrapper} event which was received
     *            by the Redis client
     * @param channel
     *            the on which the event was received
     */
    public void handleMessage(final DistributedEvent event, final String channel) {
        LOGGER.trace("retrieving event from redis {} on channel {}, posting to the local event bus", event, channel);
        if (!NODE_ID.equals(event.getOriginNodeId())) {
            event.setNodeId(NODE_ID);
            eventBus.post(event);
        }
    }

    /**
     * @return a collection of all topics which this Redis message listener
     *         wants to subscribe
     */
    public Collection<Topic> getTopics() {
        final List<Topic> topics = new ArrayList<>();
        topics.add(new PatternTopic(SUB_DISTRIBUTION_CHANNEL));
        return topics;
    }

    private void logDistributingEvent(final Event event, final String channel) {
        LOGGER.trace("distributing event {} from node {} to topic {}", event, NODE_ID, channel);
    }

    private void logNotDistributingEvent(final Event event, final String channel) {
        LOGGER.debug("no redis template configured, event {} will not be distributed to channel {} from node {}", event,
                channel, NODE_ID);
    }

    /**
     * testing purposes.
     * 
     * @param redisTemplate
     *            the redisTemplate to set
     */
    void setRedisTemplate(final RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * testing purposes.
     * 
     * @param eventBus
     *            the eventBus to set
     */
    void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * @return the nodeId
     */
    static String getNodeId() {
        return NODE_ID;
    }
}
