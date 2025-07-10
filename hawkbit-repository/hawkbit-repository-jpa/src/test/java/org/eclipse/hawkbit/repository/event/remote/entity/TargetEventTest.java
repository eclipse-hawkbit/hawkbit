/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test TargetCreatedEvent, TargetUpdatedEvent and CancelTargetAssignmentEvent
 */
class TargetEventTest extends AbstractRemoteEntityEventTest<Target> {

    /**
     * Verifies that the target entity reloading by remote created event works
     */
    @Test
    void testTargetCreatedEvent() {
        assertAndCreateRemoteEvent(TargetCreatedEvent.class);
    }

    /**
     * Verifies that the target entity reloading by remote updated event works
     */
    @Test
    void testTargetUpdatedEvent() {
        assertAndCreateRemoteEvent(TargetUpdatedEvent.class);
    }

    @Override
    protected Target createEntity() {
        return testdataFactory.createTarget("12345");
    }
}