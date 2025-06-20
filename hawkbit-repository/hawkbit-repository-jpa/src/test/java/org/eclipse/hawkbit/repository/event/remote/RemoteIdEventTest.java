/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
  * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Entity Id Events
 */
class RemoteIdEventTest extends AbstractRemoteEventTest {

    private static final long ENTITY_ID = 1L;
    private static final String TENANT = "tenant";
    private static final Class<? extends TenantAwareBaseEntity> ENTITY_CLASS = JpaAction.class;
    private static final String NODE = "Node";
    private static final String CONTROLLER_ID = "controller911";
    private static final String ADDRESS = "amqp://anyhost";

    /**
     * Verifies that the ds id is correct reloaded
     */
    @Test
    void testDistributionSetDeletedEvent() {
        assertAndCreateRemoteEvent(DistributionSetDeletedEvent.class);
    }

    /**
     * Verifies that the ds tag id is correct reloaded
     */
    @Test
    void testDistributionSetTagDeletedEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagDeletedEvent.class);
    }

    /**
     * Verifies that the target id is correct reloaded
     */
    @Test
    void testTargetDeletedEvent() {
        final TargetDeletedEvent deletedEvent = new TargetDeletedEvent(TENANT, ENTITY_ID, CONTROLLER_ID, ADDRESS,
                ENTITY_CLASS, NODE);
        assertEntity(deletedEvent);
    }

    /**
     * Verifies that the target tag id is correct reloaded
     */
    @Test
    void testTargetTagDeletedEvent() {
        assertAndCreateRemoteEvent(TargetTagDeletedEvent.class);
    }

    /**
     * Verifies that the software module id is correct reloaded
     */
    @Test
    void testSoftwareModuleDeletedEvent() {
        assertAndCreateRemoteEvent(SoftwareModuleDeletedEvent.class);
    }

    /**
     * Verifies that the rollout id is correct reloaded
     */
    @Test
    void testRolloutDeletedEvent() {
        assertAndCreateRemoteEvent(RolloutDeletedEvent.class);
    }

    protected void assertAndCreateRemoteEvent(final Class<? extends RemoteIdEvent> eventType) {

        final Constructor<?> constructor = Arrays.stream(eventType.getDeclaredConstructors())
                .filter(con -> con.getParameterCount() == 4).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Given event is not RemoteIdEvent compatible"));

        try {
            final RemoteIdEvent event = (RemoteIdEvent) constructor.newInstance(TENANT, ENTITY_ID, ENTITY_CLASS, NODE);
            assertEntity(event);
        } catch (final ReflectiveOperationException e) {
            fail("Exception should not happen " + e.getMessage());
        }
    }

    protected void assertEntity(final RemoteIdEvent event) {
        assertThat(event.getEntityId()).isSameAs(ENTITY_ID);

        final RemoteIdEvent protoStuffEvent = createProtoStuffEvent(event);
        assertDeserializeEvent(protoStuffEvent, event);

        final RemoteIdEvent jacksonEvent = createJacksonEvent(event);
        assertDeserializeEvent(jacksonEvent, event);
    }

    private void assertDeserializeEvent(final RemoteIdEvent underTestCreatedEvent, final RemoteIdEvent event) {
        // gets added because events inherit from of java.util.EventObject
        assertThat(underTestCreatedEvent).usingRecursiveComparison().ignoringFields("source").isEqualTo(event);
    }
}
