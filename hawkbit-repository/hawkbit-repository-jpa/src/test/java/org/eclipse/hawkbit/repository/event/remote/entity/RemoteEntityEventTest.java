/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.entity.BasePropertyChangeEvent.PropertyChange;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.bus.jackson.BusJacksonAutoConfiguration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RemoteEntityEventTest extends AbstractJpaIntegrationTest {

    private AbstractMessageConverter abstractMessageConverter;

    @Before
    public void setup() throws Exception {
        final BusJacksonAutoConfiguration autoConfiguration = new BusJacksonAutoConfiguration();
        this.abstractMessageConverter = autoConfiguration.busJsonConverter();
        ((InitializingBean) abstractMessageConverter).afterPropertiesSet();
    }

    @Test
    @Description("Verifies that the target entity reloading by remote events works")
    public void reloadTargetByRemoteEvent() {

        final Target entity = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        // TargetCreatedEvent
        final TargetCreatedEvent createdEvent = new TargetCreatedEvent(entity, "Node");
        final Message<TargetCreatedEvent> targetCreateMessage = MessageBuilder.withPayload(createdEvent).build();
        final TargetCreatedEvent underTestCreatedEvent = (TargetCreatedEvent) abstractMessageConverter
                .fromMessage(targetCreateMessage, TargetCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        // TargetUpdatedEvent
        final TargetUpdatedEvent updateEvent = new TargetUpdatedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        final Message<TargetUpdatedEvent> targetUpdateMessage = MessageBuilder.withPayload(updateEvent).build();
        final TargetUpdatedEvent underTestUpdateEvent = (TargetUpdatedEvent) abstractMessageConverter
                .fromMessage(targetUpdateMessage, TargetUpdatedEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the distribution set entity reloading by remote events works")
    public void reloadDistributionSetByRemoteEvent() {

        final DistributionSet entity = distributionSetManagement
                .createDistributionSet(entityFactory.generateDistributionSet("incomplete", "2", "incomplete",
                        distributionSetManagement.findDistributionSetTypeByKey("os"), null));

        // DistributionCreatedEvent
        final DistributionCreatedEvent createdEvent = new DistributionCreatedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        Message<?> message = abstractMessageConverter.toMessage(createdEvent,
                new MessageHeaders(Collections.emptyMap()));
        final DistributionCreatedEvent underTestCreatedEvent = (DistributionCreatedEvent) abstractMessageConverter
                .fromMessage(message, DistributionCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        // DistributionSetUpdateEvent
        final DistributionSetUpdateEvent updateEvent = new DistributionSetUpdateEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        message = abstractMessageConverter.toMessage(updateEvent, new MessageHeaders(Collections.emptyMap()));
        final DistributionSetUpdateEvent underTestUpdatedEvent = (DistributionSetUpdateEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetUpdateEvent.class);
        assertThat(underTestUpdatedEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the distribution set tag entity reloading by remote events works")
    public void reloadDistributionSetTagByRemoteEvent() {

        final DistributionSetTag entity = tagManagement
                .createDistributionSetTag(entityFactory.generateDistributionSetTag("tag1"));

        // DistributionSetTagUpdateEvent
        final DistributionSetTagUpdateEvent updateEvent = new DistributionSetTagUpdateEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        Message<?> message = abstractMessageConverter.toMessage(updateEvent,
                new MessageHeaders(Collections.emptyMap()));
        final DistributionSetTagUpdateEvent underTestUpdateEvent = (DistributionSetTagUpdateEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagUpdateEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);

        // DistributionSetTagDeletedEvent
        final DistributionSetTagDeletedEvent deleteEvent = new DistributionSetTagDeletedEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        message = abstractMessageConverter.toMessage(deleteEvent, new MessageHeaders(Collections.emptyMap()));
        final DistributionSetTagDeletedEvent underTestDeleteEvent = (DistributionSetTagDeletedEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagDeletedEvent.class);
        assertThat(underTestDeleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the target tag entity reloading by remote events works")
    public void reloadTargetTagTagByRemoteEvent() {

        final TargetTag entity = tagManagement.createTargetTag(entityFactory.generateTargetTag("tag1"));

        // TargetTagUpdateEvent
        final TargetTagUpdateEvent updateEvent = new TargetTagUpdateEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        Message<?> message = abstractMessageConverter.toMessage(updateEvent,
                new MessageHeaders(Collections.emptyMap()));
        final TargetTagUpdateEvent underTestUpdateEvent = (TargetTagUpdateEvent) abstractMessageConverter
                .fromMessage(message, TargetTagUpdateEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);

        // TargetTagDeletedEvent
        final TargetTagDeletedEvent deleteEvent = new TargetTagDeletedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        message = abstractMessageConverter.toMessage(deleteEvent, new MessageHeaders(Collections.emptyMap()));
        final TargetTagDeletedEvent underTestDeleteEvent = (TargetTagDeletedEvent) abstractMessageConverter
                .fromMessage(message, TargetTagDeletedEvent.class);
        assertThat(underTestDeleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the action entity reloading by remote events works")
    public void reloadActionByRemoteEvent() {

        final JpaAction generateAction = (JpaAction) entityFactory.generateAction();
        generateAction.setActionType(ActionType.FORCED);
        final Action entity = actionRepository.save(generateAction);

        // ActionCreatedEvent
        final ActionCreatedEvent createdEvent = new ActionCreatedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        Message<?> message = abstractMessageConverter.toMessage(createdEvent,
                new MessageHeaders(Collections.emptyMap()));
        final ActionCreatedEvent underTestCreatedEvent = (ActionCreatedEvent) abstractMessageConverter
                .fromMessage(message, ActionCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        // ActionPropertyChangeEvent
        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final ActionPropertyChangeEvent actionPropertyChangeEvent = new ActionPropertyChangeEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), changeSetValues, "Node");
        message = abstractMessageConverter.toMessage(actionPropertyChangeEvent,
                new MessageHeaders(Collections.emptyMap()));
        final ActionPropertyChangeEvent underTestChangeEvent = (ActionPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, ActionPropertyChangeEvent.class);
        assertThat(underTestChangeEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the rollout and rolloutgroup entity reloading by remote events works")
    public void reloadRolloutByRemoteEvent() {

        targetManagement.createTarget(entityFactory.generateTarget("12345"));
        final Rollout rollout = entityFactory.generateRollout();
        rollout.setName("exampleRollout");
        rollout.setTargetFilterQuery("controllerId==*");

        final JpaRollout entity = (JpaRollout) rolloutManagement.createRollout(rollout, 10,
                new RolloutGroupConditionBuilder().successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10")
                        .build());

        // RolloutPropertyChangeEvent
        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final RolloutPropertyChangeEvent actionPropertyChangeEvent = new RolloutPropertyChangeEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), changeSetValues, "Node");
        Message<?> message = abstractMessageConverter.toMessage(actionPropertyChangeEvent,
                new MessageHeaders(Collections.emptyMap()));
        final RolloutPropertyChangeEvent underTestRolloutPropertyChangeEvent = (RolloutPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, RolloutPropertyChangeEvent.class);
        final Rollout rolloutReload = underTestRolloutPropertyChangeEvent.getEntity();
        assertThat(rolloutReload).isEqualTo(entity);

        // RolloutGroupPropertyChangeEvent
        final RolloutGroup rolloutGroup = rolloutReload.getRolloutGroups().get(0);
        final RolloutGroupPropertyChangeEvent rolloutGroupPropertyChangeEvent = new RolloutGroupPropertyChangeEvent(
                rolloutGroup.getTenant(), rolloutGroup.getId(), rolloutGroup.getClass(), changeSetValues, "Node");
        message = abstractMessageConverter.toMessage(rolloutGroupPropertyChangeEvent,
                new MessageHeaders(Collections.emptyMap()));
        final RolloutGroupPropertyChangeEvent underTestRolloutGroupPropertyChangeEvent = (RolloutGroupPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, RolloutGroupPropertyChangeEvent.class);
        assertThat(underTestRolloutGroupPropertyChangeEvent.getEntity()).isEqualTo(rolloutGroup);
    }

}
