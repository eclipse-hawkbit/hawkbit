/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static String TENANT = "tenant";

    private static String ENTIY_CLASS = "EntityClass";

    private static String NODE = "Node";

    private static String CONTROLLER_ID = "controller911";

    private static String ADDRESS = "amqp://anyhost";

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
        final TargetDeletedEvent deletedEvent = new TargetDeletedEvent(TENANT, ENTITY_ID, CONTROLLER_ID, ADDRESS,
                ENTIY_CLASS, NODE);
        assertEntity(deletedEvent);
    }

    @Test
    @Description("Verifies that the target tag id is correct reloaded")
    public void testTargetTagDeletedEvent() {
        assertAndCreateRemoteEvent(TargetTagDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the software module id is correct reloaded")
    public void testSoftwareModuleDeletedEvent() {
        assertAndCreateRemoteEvent(SoftwareModuleDeletedEvent.class);
    }

    @Test
    @Description("Verifies that the rollout id is correct reloaded")
    public void testRolloutDeletedEvent() {
        assertAndCreateRemoteEvent(RolloutDeletedEvent.class);
    }

    protected void assertAndCreateRemoteEvent(final Class<? extends RemoteIdEvent> eventType) {

        final Constructor<?> constructor = Arrays.stream(eventType.getDeclaredConstructors())
                .filter(con -> con.getParameterCount() == 4).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Given event is not RemoteIdEvent compatible"));

        try {
            final RemoteIdEvent event = (RemoteIdEvent) constructor.newInstance(TENANT, ENTITY_ID, ENTIY_CLASS, NODE);
            assertEntity(event);
        } catch (final ReflectiveOperationException e) {
            fail("Exception should not happen " + e.getMessage());
        }
    }

    protected void assertEntity(final RemoteIdEvent event) {
        assertThat(event.getEntityId()).isSameAs(ENTITY_ID);

        final RemoteIdEvent protoStuffEvent = (RemoteIdEvent) createProtoStuffEvent(event);
        assertDeserializeEvent(protoStuffEvent, event);

        final RemoteIdEvent jacksonEvent = (RemoteIdEvent) createJacksonEvent(event);
        assertDeserializeEvent(jacksonEvent, event);
    }

    private void assertDeserializeEvent(final RemoteIdEvent underTestCreatedEvent, final RemoteIdEvent event) {
        assertThat(underTestCreatedEvent).isEqualToIgnoringGivenFields(event, "source");
    }

}
