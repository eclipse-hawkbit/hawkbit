/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.event.RepositoryEntityEventTest.RepositoryTestConfiguration;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@Feature("Component Tests - Repository")
@Story("Entity Events")
@SpringBootTest(classes = { RepositoryTestConfiguration.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
public class RepositoryEntityEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private MyEventListener eventListener;

    @BeforeEach
    public void beforeTest() {
        eventListener.queue.clear();
    }

    @Test
    @Description("Verifies that the target created event is published when a target has been created")
    public void targetCreatedEventIsPublished() throws InterruptedException {
        final Target createdTarget = testdataFactory.createTarget("12345");

        final TargetCreatedEvent targetCreatedEvent = eventListener.waitForEvent(TargetCreatedEvent.class);
        assertThat(targetCreatedEvent).isNotNull();
        assertThat(getIdOfEntity(targetCreatedEvent)).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target update event is published when a target has been updated")
    public void targetUpdateEventIsPublished() throws InterruptedException {
        final Target createdTarget = testdataFactory.createTarget("12345");
        targetManagement.update(entityFactory.target().update(createdTarget.getControllerId()).name("updateName"));

        final TargetUpdatedEvent targetUpdatedEvent = eventListener.waitForEvent(TargetUpdatedEvent.class);
        assertThat(targetUpdatedEvent).isNotNull();
        assertThat(getIdOfEntity(targetUpdatedEvent)).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target deleted event is published when a target has been deleted")
    public void targetDeletedEventIsPublished() throws InterruptedException {
        final Target createdTarget = testdataFactory.createTarget("12345");

        targetManagement.deleteByControllerID("12345");

        final TargetDeletedEvent targetDeletedEvent = eventListener.waitForEvent(TargetDeletedEvent.class);
        assertThat(targetDeletedEvent).isNotNull();
        assertThat(targetDeletedEvent.getEntityId()).isEqualTo(createdTarget.getId());
    }

    @Test
    @Description("Verifies that the target type created event is published when a target type has been created")
    public void targetTypeCreatedEventIsPublished() throws InterruptedException {
        final TargetType createdTargetType = testdataFactory.findOrCreateTargetType("targettype");

        final TargetTypeCreatedEvent targetTypeCreatedEvent = eventListener.waitForEvent(TargetTypeCreatedEvent.class);
        assertThat(targetTypeCreatedEvent).isNotNull();
        assertThat(getIdOfEntity(targetTypeCreatedEvent)).isEqualTo(createdTargetType.getId());
    }

    @Test
    @Description("Verifies that the target type updated event is published when a target type has been updated")
    public void targetTypeUpdatedEventIsPublished() throws InterruptedException {
        final TargetType createdTargetType = testdataFactory.findOrCreateTargetType("targettype");
        targetTypeManagement
                .update(entityFactory.targetType().update(createdTargetType.getId()).name("updatedtargettype"));

        final TargetTypeUpdatedEvent targetTypeUpdatedEvent = eventListener.waitForEvent(TargetTypeUpdatedEvent.class);
        assertThat(targetTypeUpdatedEvent).isNotNull();
        assertThat(getIdOfEntity(targetTypeUpdatedEvent)).isEqualTo(createdTargetType.getId());
    }

    @Test
    @Description("Verifies that the target type deleted event is published when a target type has been deleted")
    public void targetTypeDeletedEventIsPublished() throws InterruptedException {
        final TargetType createdTargetType = testdataFactory.findOrCreateTargetType("targettype");
        targetTypeManagement.delete(createdTargetType.getId());

        final TargetTypeDeletedEvent targetTypeDeletedEvent = eventListener.waitForEvent(TargetTypeDeletedEvent.class);
        assertThat(targetTypeDeletedEvent).isNotNull();
        assertThat(targetTypeDeletedEvent.getEntityId()).isEqualTo(createdTargetType.getId());
    }

    @Test
    @Description("Verifies that the rollout deleted event is published when a rollout has been deleted")
    public void rolloutDeletedEventIsPublished() throws InterruptedException {
        final int amountTargetsForRollout = 50;
        final int amountGroups = 5;
        final String successCondition = "50";
        final String errorCondition = "80";
        final String rolloutName = "rolloutTest";
        final String targetPrefixName = rolloutName;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsFor" + rolloutName);
        testdataFactory.createTargets(amountTargetsForRollout, targetPrefixName + "-", targetPrefixName);

        final Rollout createdRollout = testdataFactory.createRolloutByVariables(rolloutName, "desc", amountGroups,
                "controllerId==" + targetPrefixName + "-*", distributionSet, successCondition, errorCondition);

        rolloutManagement.delete(createdRollout.getId());
        rolloutHandler.handleAll();

        final RolloutDeletedEvent rolloutDeletedEvent = eventListener.waitForEvent(RolloutDeletedEvent.class);
        assertThat(rolloutDeletedEvent).isNotNull();
        assertThat(rolloutDeletedEvent.getEntityId()).isEqualTo(createdRollout.getId());
    }

    @Test
    @Description("Verifies that the distribution set created event is published when a distribution set has been created")
    public void distributionSetCreatedEventIsPublished() throws InterruptedException {
        final DistributionSet createDistributionSet = testdataFactory.createDistributionSet();

        final DistributionSetCreatedEvent dsCreatedEvent = eventListener
                .waitForEvent(DistributionSetCreatedEvent.class);
        assertThat(dsCreatedEvent).isNotNull();
        assertThat(getIdOfEntity(dsCreatedEvent)).isEqualTo(createDistributionSet.getId());
    }

    @Test
    @Description("Verifies that the distribution set deleted event is published when a distribution set has been deleted")
    public void distributionSetDeletedEventIsPublished() throws InterruptedException {
        final DistributionSet createDistributionSet = testdataFactory.createDistributionSet();

        distributionSetManagement.delete(createDistributionSet.getId());

        final DistributionSetDeletedEvent dsDeletedEvent = eventListener
                .waitForEvent(DistributionSetDeletedEvent.class);
        assertThat(dsDeletedEvent).isNotNull();
        assertThat(dsDeletedEvent.getEntityId()).isEqualTo(createDistributionSet.getId());
    }

    @Test
    @Description("Verifies that the software module created event is published when a software module has been created")
    public void softwareModuleCreatedEventIsPublished() throws InterruptedException {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleApp();

        final SoftwareModuleCreatedEvent softwareModuleCreatedEvent = eventListener
                .waitForEvent(SoftwareModuleCreatedEvent.class);
        assertThat(softwareModuleCreatedEvent).isNotNull();
        assertThat(getIdOfEntity(softwareModuleCreatedEvent)).isEqualTo(softwareModule.getId());
    }

    @Test
    @Description("Verifies that the software module update event is published when a software module has been updated")
    public void softwareModuleUpdateEventIsPublished() throws InterruptedException {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleApp();
        softwareModuleManagement
                .update(entityFactory.softwareModule().update(softwareModule.getId()).description("New"));

        final SoftwareModuleUpdatedEvent softwareModuleUpdatedEvent = eventListener
                .waitForEvent(SoftwareModuleUpdatedEvent.class);
        assertThat(softwareModuleUpdatedEvent).isNotNull();
        assertThat(getIdOfEntity(softwareModuleUpdatedEvent)).isEqualTo(softwareModule.getId());
    }

    @Test
    @Description("Verifies that the software module deleted event is published when a software module has been deleted")
    public void softwareModuleDeletedEventIsPublished() throws InterruptedException {
        final SoftwareModule softwareModule = testdataFactory.createSoftwareModuleApp();
        softwareModuleManagement.delete(softwareModule.getId());

        final SoftwareModuleDeletedEvent softwareModuleDeletedEvent = eventListener
                .waitForEvent(SoftwareModuleDeletedEvent.class);
        assertThat(softwareModuleDeletedEvent).isNotNull();
        assertThat(softwareModuleDeletedEvent.getEntityId()).isEqualTo(softwareModule.getId());
    }

    private static Long getIdOfEntity(final RemoteEntityEvent<?> event) {
        event.getEntityId();
        return event.getEntity().get().getId();
    }

    public static class RepositoryTestConfiguration {

        @Bean
        public MyEventListener myEventListenerBean() {
            return new MyEventListener();
        }

    }

    private static class MyEventListener {

        private final BlockingQueue<TenantAwareEvent> queue = new LinkedBlockingQueue<>();

        @EventListener(classes = TenantAwareEvent.class)
        public void onEvent(final TenantAwareEvent event) {
            queue.offer(event);
        }

        public <T> T waitForEvent(final Class<T> eventType) throws InterruptedException {
            TenantAwareEvent event = null;
            while ((event = queue.poll(5, TimeUnit.SECONDS)) != null) {
                if (event.getClass().isAssignableFrom(eventType)) {
                    return (T) event;
                }
            }
            Assertions.fail("Missing event " + eventType + " within timeout.");
            return null;
        }
    }

}
