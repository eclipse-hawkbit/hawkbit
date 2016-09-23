/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event.entity;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionPropertyChangeEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.BasePropertyChangeEvent.PropertyChange;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTagUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupPropertyChangeEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutPropertyChangeEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdateEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantAwareBaseEntityEvent;
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
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.bus.jackson.BusJacksonAutoConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

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
        ReflectionTestUtils.setField(abstractMessageConverter, "packagesToScan",
                new String[] { "org.eclipse.hawkbit.repository.even.remote",
                        ClassUtils.getPackageName(RemoteApplicationEvent.class) });
        ((InitializingBean) abstractMessageConverter).afterPropertiesSet();

    }

    @Test
    @Description("Verifies that the target entity reloading by remote events works")
    public void reloadTargetByRemoteEvent() throws JsonProcessingException {

        final Target entity = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        final TargetCreatedEvent createdEvent = new TargetCreatedEvent(entity, "Node");
        assertThat(createdEvent.getEntity()).isSameAs(entity);
        final Message<String> targetCreateMessage = createMessage(createdEvent);
        final TargetCreatedEvent underTestCreatedEvent = (TargetCreatedEvent) abstractMessageConverter
                .fromMessage(targetCreateMessage, TargetCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        final TargetUpdatedEvent updateEvent = new TargetUpdatedEvent(entity, "Node");
        assertThat(updateEvent.getEntity()).isSameAs(entity);
        final Message<String> targetUpdateMessage = createMessage(updateEvent);
        final TargetUpdatedEvent underTestUpdateEvent = (TargetUpdatedEvent) abstractMessageConverter
                .fromMessage(targetUpdateMessage, TargetUpdatedEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the distribution set entity reloading by remote events works")
    public void reloadDistributionSetByRemoteEvent() throws JsonProcessingException {

        final DistributionSet entity = distributionSetManagement
                .createDistributionSet(entityFactory.generateDistributionSet("incomplete", "2", "incomplete",
                        distributionSetManagement.findDistributionSetTypeByKey("os"), null));

        final DistributionCreatedEvent createdEvent = new DistributionCreatedEvent(entity, "Node");
        assertThat(createdEvent.getEntity()).isSameAs(entity);
        Message<?> message = createMessage(createdEvent);
        final DistributionCreatedEvent underTestCreatedEvent = (DistributionCreatedEvent) abstractMessageConverter
                .fromMessage(message, DistributionCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        final DistributionSetUpdateEvent updateEvent = new DistributionSetUpdateEvent(entity, "Node");
        assertThat(updateEvent.getEntity()).isSameAs(entity);
        message = createMessage(updateEvent);
        final DistributionSetUpdateEvent underTestUpdatedEvent = (DistributionSetUpdateEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetUpdateEvent.class);
        assertThat(underTestUpdatedEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the distribution set tag entity reloading by remote events works")
    public void reloadDistributionSetTagByRemoteEvent() throws JsonProcessingException {

        final DistributionSetTag entity = tagManagement
                .createDistributionSetTag(entityFactory.generateDistributionSetTag("tag1"));

        final DistributionSetTagUpdateEvent updateEvent = new DistributionSetTagUpdateEvent(entity, "Node");
        assertThat(updateEvent.getEntity()).isSameAs(entity);
        Message<?> message = createMessage(updateEvent);
        final DistributionSetTagUpdateEvent underTestUpdateEvent = (DistributionSetTagUpdateEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagUpdateEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);

        // DistributionSetTagDeletedEvent
        final DistributionSetTagDeletedEvent deleteEvent = new DistributionSetTagDeletedEvent(entity, "Node");
        assertThat(deleteEvent.getEntity()).isSameAs(entity);
        message = createMessage(deleteEvent);
        final DistributionSetTagDeletedEvent underTestDeleteEvent = (DistributionSetTagDeletedEvent) abstractMessageConverter
                .fromMessage(message, DistributionSetTagDeletedEvent.class);
        assertThat(underTestDeleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the target tag entity reloading by remote events works")
    public void reloadTargetTagTagByRemoteEvent() throws JsonProcessingException {

        final TargetTag entity = tagManagement.createTargetTag(entityFactory.generateTargetTag("tag1"));

        // TargetTagUpdateEvent
        final TargetTagUpdateEvent updateEvent = new TargetTagUpdateEvent(entity, "Node");
        assertThat(updateEvent.getEntity()).isSameAs(entity);
        Message<?> message = createMessage(updateEvent);
        final TargetTagUpdateEvent underTestUpdateEvent = (TargetTagUpdateEvent) abstractMessageConverter
                .fromMessage(message, TargetTagUpdateEvent.class);
        assertThat(underTestUpdateEvent.getEntity()).isEqualTo(entity);

        // TargetTagDeletedEvent
        final TargetTagDeletedEvent deleteEvent = new TargetTagDeletedEvent(entity, "Node");
        assertThat(deleteEvent.getEntity()).isSameAs(entity);
        message = createMessage(deleteEvent);
        final TargetTagDeletedEvent underTestDeleteEvent = (TargetTagDeletedEvent) abstractMessageConverter
                .fromMessage(message, TargetTagDeletedEvent.class);
        assertThat(underTestDeleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the action entity reloading by remote events works")
    public void reloadActionByRemoteEvent() throws JsonProcessingException {

        final JpaAction generateAction = (JpaAction) entityFactory.generateAction();
        generateAction.setActionType(ActionType.FORCED);
        final Action entity = actionRepository.save(generateAction);

        final ActionCreatedEvent createdEvent = new ActionCreatedEvent(entity, "Node");
        assertThat(createdEvent.getEntity()).isSameAs(entity);
        Message<?> message = createMessage(createdEvent);
        final ActionCreatedEvent underTestCreatedEvent = (ActionCreatedEvent) abstractMessageConverter
                .fromMessage(message, ActionCreatedEvent.class);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(entity);

        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final PropertyChange value = new PropertyChange("TEST", "TEST1");
        changeSetValues.put("TEST", value);
        final ActionPropertyChangeEvent actionPropertyChangeEvent = new ActionPropertyChangeEvent(entity,
                changeSetValues, "Node");
        assertThat(actionPropertyChangeEvent.getEntity()).isSameAs(entity);
        message = createMessage(actionPropertyChangeEvent);
        final ActionPropertyChangeEvent underTestChangeEvent = (ActionPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, ActionPropertyChangeEvent.class);
        assertThat(underTestChangeEvent.getEntity()).isEqualTo(entity);
        assertThat(underTestChangeEvent.getChangeSetValues()).isNotEmpty();
        assertThat(underTestChangeEvent.getChangeSetValues().get("TEST")).isEqualTo(value);
    }

    @Test
    @Description("Verifies that the rollout and rolloutgroup entity reloading by remote events works")
    public void reloadRolloutByRemoteEvent() throws JsonProcessingException {

        targetManagement.createTarget(entityFactory.generateTarget("12345"));
        final Rollout rollout = entityFactory.generateRollout();
        rollout.setName("exampleRollout");
        rollout.setTargetFilterQuery("controllerId==*");

        final JpaRollout entity = (JpaRollout) rolloutManagement.createRollout(rollout, 10,
                new RolloutGroupConditionBuilder().successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10")
                        .build());

        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final RolloutPropertyChangeEvent propertyChangeEvent = new RolloutPropertyChangeEvent(entity, changeSetValues,
                "Node");
        assertThat(propertyChangeEvent.getEntity()).isSameAs(entity);
        Message<?> message = createMessage(propertyChangeEvent);

        final RolloutPropertyChangeEvent underTestRolloutPropertyChangeEvent = (RolloutPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, RolloutPropertyChangeEvent.class);
        final Rollout rolloutReload = underTestRolloutPropertyChangeEvent.getEntity();
        assertThat(rolloutReload).isEqualTo(entity);
        assertThat(underTestRolloutPropertyChangeEvent.getChangeSetValues()).isNotNull();

        final RolloutGroup rolloutGroup = rolloutReload.getRolloutGroups().get(0);
        final RolloutGroupPropertyChangeEvent rolloutGroupPropertyChangeEvent = new RolloutGroupPropertyChangeEvent(
                rolloutGroup, changeSetValues, "Node");
        message = createMessage(rolloutGroupPropertyChangeEvent);
        final RolloutGroupPropertyChangeEvent underTestRolloutGroupPropertyChangeEvent = (RolloutGroupPropertyChangeEvent) abstractMessageConverter
                .fromMessage(message, RolloutGroupPropertyChangeEvent.class);
        assertThat(underTestRolloutGroupPropertyChangeEvent.getEntity()).isEqualTo(rolloutGroup);
        assertThat(underTestRolloutGroupPropertyChangeEvent.getChangeSetValues()).isNotNull();
    }

    private Message<String> createMessage(final TenantAwareBaseEntityEvent<?> event) throws JsonProcessingException {
        final Map<String, MimeType> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
        final String json = new ObjectMapper().writeValueAsString(event);
        final Message<String> message = MessageBuilder.withPayload(json).copyHeaders(headers).build();
        return message;
    }

}
