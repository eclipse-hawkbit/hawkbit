/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.eventbus;

import static org.fest.assertions.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.event.entity.DistributionCreatedEvent;
import org.springframework.cloud.bus.event.entity.DistributionDeletedEvent;
import org.springframework.cloud.bus.event.entity.TargetCreatedEvent;
import org.springframework.cloud.bus.event.entity.TargetDeletedEvent;
import org.springframework.cloud.bus.event.entity.TargetInfoUpdateEvent;
import org.springframework.cloud.bus.event.entity.TargetUpdatedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RepositoryEntityEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private EventBus eventBus;

    private final MyEventListener eventListener = new MyEventListener();

    @Before
    public void beforeTest() {
        eventListener.queue.clear();
        eventBus.register(eventListener);
    }

    @Test
    @Description("Verifies that the target created event is published when a target has been created")
    public void targetCreatedEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        final TargetCreatedEvent targetCreatedEvent = eventListener.waitForEvent(TargetCreatedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetCreatedEvent).isNotNull();
        assertThat(targetCreatedEvent.getEntity().getId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target update event is published when a target has been updated")
    public void targetUpdateEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));
        createdTarget.setName("updateName");
        targetManagement.updateTarget(createdTarget);

        final TargetUpdatedEvent targetUpdatedEvent = eventListener.waitForEvent(TargetUpdatedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetUpdatedEvent).isNotNull();
        assertThat(targetUpdatedEvent.getEntity().getId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target info update event is published when a target info has been updated")
    public void targetInfoUpdateEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));
        controllerManagament.updateTargetStatus(createdTarget.getTargetInfo(), TargetUpdateStatus.PENDING,
                System.currentTimeMillis(), URI.create("http://127.0.0.1"));

        final TargetInfoUpdateEvent targetInfoUpdatedEvent = eventListener.waitForEvent(TargetInfoUpdateEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetInfoUpdatedEvent).isNotNull();
        assertThat(targetInfoUpdatedEvent.getEntity().getTarget().getId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target deleted event is published when a target has been deleted")
    public void targetDeletedEventIsPublished() throws InterruptedException {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        targetManagement.deleteTargets(createdTarget.getId());

        final TargetDeletedEvent targetDeletedEvent = eventListener.waitForEvent(TargetDeletedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(targetDeletedEvent).isNotNull();
        assertThat(targetDeletedEvent.getEntityId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the distribution set created event is published when a distribution set has been created")
    public void distributionSetCreatedEventIsPublished() throws InterruptedException {
        final DistributionSet generateDistributionSet = entityFactory.generateDistributionSet();
        generateDistributionSet.setName("dsEventTest");
        generateDistributionSet.setVersion("1");
        final DistributionSet createDistributionSet = distributionSetManagement
                .createDistributionSet(generateDistributionSet);

        final DistributionCreatedEvent dsCreatedEvent = eventListener.waitForEvent(DistributionCreatedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(dsCreatedEvent).isNotNull();
        assertThat(dsCreatedEvent.getEntity().getId()).isEqualTo(createDistributionSet.getId());
    }

    @Test
    @Description("Verifies that the distribution set deleted event is published when a distribution set has been deleted")
    public void distributionSetDeletedEventIsPublished() throws InterruptedException {

        final DistributionSet generateDistributionSet = entityFactory.generateDistributionSet();
        generateDistributionSet.setName("dsEventTest");
        generateDistributionSet.setVersion("1");
        final DistributionSet createDistributionSet = distributionSetManagement
                .createDistributionSet(generateDistributionSet);

        distributionSetManagement.deleteDistributionSet(createDistributionSet);

        final DistributionDeletedEvent dsDeletedEvent = eventListener.waitForEvent(DistributionDeletedEvent.class, 1,
                TimeUnit.SECONDS);
        assertThat(dsDeletedEvent).isNotNull();
        assertThat(dsDeletedEvent.getEntityId()).isEqualTo(createDistributionSet.getId());
    }

    private class MyEventListener {

        private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();

        @Subscribe
        public void onEvent(final Event event) {
            queue.offer(event);
        }

        public <T> T waitForEvent(final Class<T> eventType, final long timeout, final TimeUnit timeUnit)
                throws InterruptedException {
            Event event = null;
            while ((event = queue.poll(timeout, timeUnit)) != null) {
                if (event.getClass().isAssignableFrom(eventType)) {
                    return (T) event;
                }
            }
            Assertions.fail("Missing event " + eventType + " within timeout " + timeout + " " + timeUnit);
            return null;
        }
    }

}
