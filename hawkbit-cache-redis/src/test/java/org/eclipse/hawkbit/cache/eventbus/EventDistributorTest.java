/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache.eventbus;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collection;

import org.eclipse.hawkbit.eventbus.event.DownloadProgressEvent;
import org.eclipse.hawkbit.eventbus.event.EntityEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.Topic;
import org.springframework.hateoas.Identifiable;

import com.google.common.eventbus.EventBus;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Cluster Cache")
@Stories("EventDistributor Test")
@RunWith(MockitoJUnitRunner.class)
// TODO: create description annotations
public class EventDistributorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplateMock;

    @Mock
    private EventBus eventBusMock;

    private EventDistributor underTest;

    @Before
    public void before() {
        underTest = new EventDistributor();
        underTest.setEventBus(eventBusMock);
        underTest.setRedisTemplate(redisTemplateMock);
    }

    @Test
    public void distributeDistributedEventSendsToRedis() {

        final DownloadProgressEvent event = new DownloadProgressEvent("tenant", 123L, 10, 100L, 200L);
        underTest.distribute(event);

        // origin node ID should be set by distributing the event
        assertThat(event.getOriginNodeId()).isNotNull();
        verify(redisTemplateMock).convertAndSend(anyString(), eq(event));
    }

    @Test
    public void dontDistributeDistributedEventIfSameNode() {
        final String knownNodeId = EventDistributor.getNodeId();
        final DownloadProgressEvent event = new DownloadProgressEvent("tenant", 123L, 10, 100L, 200L);
        event.setNodeId(knownNodeId);

        // test
        underTest.distribute(event);

        assertThat(event.getOriginNodeId()).isNull();
        verify(redisTemplateMock, times(0)).convertAndSend(anyString(), eq(event));
    }

    @Test
    public void handleDistributedMessageFromRedis() {
        final DownloadProgressEvent event = new DownloadProgressEvent("tenant", 123L, 10, 100L, 200L);
        final String knownChannel = "someChannel";

        underTest.handleMessage(event, knownChannel);

        assertThat(event.getNodeId()).isEqualTo(EventDistributor.getNodeId());
        verify(eventBusMock).post(eq(event));
    }

    @Test
    public void handleDistributedMessageFilteredIfSameNodeId() {
        final DownloadProgressEvent event = new DownloadProgressEvent("tenant", 123L, 10, 100L, 200L);
        final String knownChannel = "someChannel";
        event.setOriginNodeId(EventDistributor.getNodeId());

        underTest.handleMessage(event, knownChannel);

        assertThat(event.getNodeId()).isNull();
        verify(eventBusMock, times(0)).post(eq(event));
    }

    @Test
    public void subscribedTopicsContains2PatternTopics() {
        final Collection<Topic> topics = underTest.getTopics();
        assertThat(topics).hasSize(1);
    }

    private class TestEntityEvent implements EntityEvent {

        private String originNodeId;
        private String nodeId;
        private final String tenant;
        private final MyEntity entity;

        /**
         * @param myEntity
         */
        public TestEntityEvent(final MyEntity myEntity, final String tenant) {
            this.entity = myEntity;
            this.tenant = tenant;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.hawkbit.server.eventbus.event.Event#getRevision()
         */
        @Override
        public long getRevision() {
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.NodeAware#getOriginNodeId()
         */
        @Override
        public String getOriginNodeId() {
            return originNodeId;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.NodeAware#setOriginNodeId(
         * java. lang.String)
         */
        @Override
        public void setOriginNodeId(final String originNodeId) {
            this.originNodeId = originNodeId;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.hawkbit.server.eventbus.event.NodeAware#getNodeId()
         */
        @Override
        public String getNodeId() {
            return nodeId;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.NodeAware#setNodeId(java.
         * lang. String)
         */
        @Override
        public void setNodeId(final String nodeId) {
            this.nodeId = nodeId;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getEntity(java.
         * lang. Class)
         */
        @Override
        public <E> E getEntity(final Class<E> entityClass) {
            return entityClass.cast(entity);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getEntity()
         */
        @Override
        public Object getEntity() {
            return entity;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.hawkbit.server.eventbus.event.EntityEvent#getTenant()
         */
        @Override
        public String getTenant() {
            return tenant;
        }
    }

    private class MyEntity implements Identifiable<String> {
        private final String id = "123";

        /*
         * (non-Javadoc)
         *
         * @see org.springframework.hateoas.Identifiable#getId()
         */
        @Override
        public String getId() {
            return id;
        }
    }

}
