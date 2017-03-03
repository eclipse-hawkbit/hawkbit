/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test ActionCreatedEvent and ActionUpdatedEvent")
public class ActionEventTest extends AbstractRemoteEntityEventTest<Action> {

    @Test
    @Description("Verifies that the action entity reloading by remote created works")
    public void testActionCreatedEvent() {
        assertAndCreateRemoteEvent(ActionCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the action entity reloading by remote updated works")
    public void testActionUpdatedEvent() {
        assertAndCreateRemoteEvent(ActionUpdatedEvent.class);
    }

    @Override
    protected RemoteEntityEvent<?> createRemoteEvent(final Action baseEntity,
            final Class<? extends RemoteEntityEvent<?>> eventType) {

        Constructor<?> constructor = null;
        for (final Constructor<?> constructors : eventType.getDeclaredConstructors()) {
            if (constructors.getParameterCount() == 4) {
                constructor = constructors;
            }
        }

        if (constructor == null) {
            throw new IllegalArgumentException("No suitable constructor foundes");
        }

        try {
            return (RemoteEntityEvent<?>) constructor.newInstance(baseEntity, 1L, 2L, "Node");
        } catch (final ReflectiveOperationException e) {
            fail("Exception should not happen " + e.getMessage());
        }
        return null;
    }

    @Override
    protected RemoteEntityEvent<?> assertEntity(final Action baseEntity, final RemoteEntityEvent<?> e) {
        final AbstractActionEvent event = (AbstractActionEvent) e;

        assertThat(event.getEntity()).isSameAs(baseEntity);
        assertThat(event.getRolloutId()).isEqualTo(1L);

        AbstractActionEvent underTestCreatedEvent = (AbstractActionEvent) createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutGroupId()).isEqualTo(2L);

        underTestCreatedEvent = (AbstractActionEvent) createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutGroupId()).isEqualTo(2L);

        return underTestCreatedEvent;
    }

    @Override
    protected Action createEntity() {
        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        final Target target = testdataFactory.createTarget("Test");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        generateAction.setTarget(target);
        generateAction.setDistributionSet(distributionSet);
        generateAction.setStatus(Status.RUNNING);
        return actionRepository.save(generateAction);
    }

}
