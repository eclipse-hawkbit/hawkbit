/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.junit.Test;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Id Events")
public class RemoteIdEventTest extends AbstractRemoteEventTest {

    @Test
    @Description("Verifies that the is ds id correct reloaded")
    public void testDistributionSetDeletedEvent() {
        assertAndCreateRemoteEvent(DistributionSetDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the ds tag id is correct reloaded")
    public void testDistributionSetTagDeletedEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the target id is correct reloaded")
    public void testTargetDeletedEvent() {
        assertAndCreateRemoteEvent(TargetDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the target tag id is correct reloaded")
    public void testTargetTagDeletedEvent() {
        assertAndCreateRemoteEvent(TargetTagDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the target info update reload the entity correct")
    public void testTargetInfoUpdateEvent() throws JsonProcessingException {
        final Target target = targetManagement.createTarget(entityFactory.generateTarget("test"));
        final TargetInfo targetInfo = targetManagement.findTargetByControllerID(target.getControllerId())
                .getTargetInfo();
        final TargetInfoUpdateEvent infoUpdateEvent = new TargetInfoUpdateEvent(targetInfo, "node");
        final TargetInfoUpdateEvent underTest = (TargetInfoUpdateEvent) assertEntity(target.getId(), infoUpdateEvent);

        assertThat(underTest.getEntity().getTarget().getControllerId())
                .isEqualTo(targetInfo.getTarget().getControllerId());
        assertThat(underTest.getEntity()).isEqualTo(targetInfo);
    }

    protected void assertAndCreateRemoteEvent(final Class<? extends RemoteIdEvent> eventType) {

        final Long entityId = 1L;

        final Constructor<?> constructor = eventType.getDeclaredConstructors()[0];

        try {
            final RemoteIdEvent event = (RemoteIdEvent) constructor.newInstance("tenant", entityId, "Node");
            assertEntity(entityId, event);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | JsonProcessingException e) {
            fail("Exception should not happen " + e.getMessage());
        }
    }

    protected RemoteIdEvent assertEntity(final Long id, final RemoteIdEvent event) throws JsonProcessingException {
        assertThat(event.getEntityId()).isSameAs(id);

        final Message<?> message = createMessage(event);
        final RemoteIdEvent underTestCreatedEvent = (RemoteIdEvent) getAbstractMessageConverter().fromMessage(message,
                event.getClass());
        assertThat(underTestCreatedEvent.getEntityId()).isEqualTo(id);
        return underTestCreatedEvent;
    }

}
