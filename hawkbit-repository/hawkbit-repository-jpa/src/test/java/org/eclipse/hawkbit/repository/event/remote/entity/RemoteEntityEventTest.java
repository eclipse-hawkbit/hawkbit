/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.fest.assertions.Assertions.assertThat;

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
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public class RemoteEntityEventTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Verifies that the target entity reloading by remote events works")
    public void reloadTargetByRemoteEvent() {
        final Target createdTarget = targetManagement.createTarget(entityFactory.generateTarget("12345"));

        final TargetCreatedEvent createdEvent = new TargetCreatedEvent(createdTarget.getTenant(), createdTarget.getId(),
                createdTarget.getClass(), "Node");
        assertThat(createdEvent.getEntity()).isEqualTo(createdTarget);

        final TargetUpdatedEvent updateEvent = new TargetUpdatedEvent(createdTarget.getTenant(), createdTarget.getId(),
                createdTarget.getClass(), "Node");
        assertThat(updateEvent.getEntity()).isEqualTo(createdTarget);
    }

    @Test
    @Description("Verifies that the distribution set entity reloading by remote events works")
    public void reloadDistributionSetByRemoteEvent() {
        final DistributionSet entity = distributionSetManagement
                .createDistributionSet(entityFactory.generateDistributionSet("incomplete", "2", "incomplete",
                        distributionSetManagement.findDistributionSetTypeByKey("os"), null));

        final DistributionCreatedEvent createdEvent = new DistributionCreatedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        assertThat(createdEvent.getEntity()).isEqualTo(entity);

        final DistributionSetUpdateEvent updateEvent = new DistributionSetUpdateEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        assertThat(updateEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the distribution set tag entity reloading by remote events works")
    public void reloadDistributionSetTagByRemoteEvent() {
        final DistributionSetTag entity = tagManagement
                .createDistributionSetTag(entityFactory.generateDistributionSetTag("tag1"));

        final DistributionSetTagUpdateEvent updateEvent = new DistributionSetTagUpdateEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        assertThat(updateEvent.getEntity()).isEqualTo(entity);

        final DistributionSetTagDeletedEvent deleteEvent = new DistributionSetTagDeletedEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), "Node");
        assertThat(deleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the target tag entity reloading by remote events works")
    public void reloadTargetTagTagByRemoteEvent() {
        final TargetTag entity = tagManagement.createTargetTag(entityFactory.generateTargetTag("tag1"));

        final TargetTagUpdateEvent updateEvent = new TargetTagUpdateEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        assertThat(updateEvent.getEntity()).isEqualTo(entity);

        final TargetTagDeletedEvent deleteEvent = new TargetTagDeletedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        assertThat(deleteEvent.getEntity()).isEqualTo(entity);
    }

    @Test
    @Description("Verifies that the action entity reloading by remote events works")
    public void reloadActionByRemoteEvent() {
        final JpaAction generateAction = (JpaAction) entityFactory.generateAction();
        generateAction.setActionType(ActionType.FORCED);
        final Action entity = actionRepository.save(generateAction);

        final ActionCreatedEvent createdEvent = new ActionCreatedEvent(entity.getTenant(), entity.getId(),
                entity.getClass(), "Node");
        assertThat(createdEvent.getEntity()).isEqualTo(entity);

        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final ActionPropertyChangeEvent actionPropertyChangeEvent = new ActionPropertyChangeEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), changeSetValues, "Node");

        assertThat(actionPropertyChangeEvent.getEntity()).isEqualTo(entity);

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

        final Map<String, PropertyChange> changeSetValues = new HashMap<>();
        final RolloutPropertyChangeEvent actionPropertyChangeEvent = new RolloutPropertyChangeEvent(entity.getTenant(),
                entity.getId(), entity.getClass(), changeSetValues, "Node");

        final Rollout rolloutReload = actionPropertyChangeEvent.getEntity();
        assertThat(rolloutReload).isEqualTo(entity);

        final RolloutGroup rolloutGroup = rolloutReload.getRolloutGroups().get(0);
        final RolloutGroupPropertyChangeEvent rolloutGroupPropertyChangeEvent = new RolloutGroupPropertyChangeEvent(
                rolloutGroup.getTenant(), rolloutGroup.getId(), rolloutGroup.getClass(), changeSetValues, "Node");
        assertThat(rolloutGroupPropertyChangeEvent.getEntity()).isEqualTo(rolloutGroup);
    }

}
