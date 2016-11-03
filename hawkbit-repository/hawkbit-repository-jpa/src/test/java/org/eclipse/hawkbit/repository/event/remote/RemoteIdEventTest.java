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
import java.util.Arrays;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Id Events")
public class RemoteIdEventTest extends AbstractRemoteEventTest {

    private static final long ENTITY_ID = 1L;

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

    protected void assertAndCreateRemoteEvent(final Class<? extends RemoteIdEvent> eventType) {

        final Constructor<?> constructor = Arrays.stream(eventType.getDeclaredConstructors())
                .filter(con -> con.getParameterCount() == 3).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Given event is not RemoteIdEvent compatible"));

        try {
            final RemoteIdEvent event = (RemoteIdEvent) constructor.newInstance("tenant", ENTITY_ID, "Node");
            assertEntity(ENTITY_ID, event);
        } catch (final ReflectiveOperationException e) {
            fail("Exception should not happen " + e.getMessage());
        }
    }

    protected RemoteIdEvent assertEntity(final long id, final RemoteIdEvent event) {
        assertThat(event.getEntityId()).isSameAs(id);

        RemoteIdEvent underTestCreatedEvent = (RemoteIdEvent) createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntityId()).isEqualTo(id);

        underTestCreatedEvent = (RemoteIdEvent) createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntityId()).isEqualTo(id);

        return underTestCreatedEvent;
    }

}
