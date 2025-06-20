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

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
  * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test TargetTagCreatedEvent and TargetTagUpdateEvent
 */
class TargetTagEventTest extends AbstractRemoteEntityEventTest<TargetTag> {

    /**
     * Verifies that the target tag entity reloading by remote created event works
     */
    @Test
    void testTargetTagCreatedEvent() {
        assertAndCreateRemoteEvent(TargetTagCreatedEvent.class);
    }

    /**
     * Verifies that the target tag entity reloading by remote updated event works
     */
    @Test
    void testTargetTagUpdateEventt() {
        assertAndCreateRemoteEvent(TargetTagUpdatedEvent.class);
    }

    @Override
    protected TargetTag createEntity() {
        return targetTagManagement.create(entityFactory.tag().create().name("tag1"));
    }

}
