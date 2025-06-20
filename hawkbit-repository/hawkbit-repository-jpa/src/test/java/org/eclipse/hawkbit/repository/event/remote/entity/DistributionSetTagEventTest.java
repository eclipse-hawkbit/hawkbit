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

import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
  * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test DistributionSetTagCreatedEvent and DistributionSetTagUpdateEvent
 */
class DistributionSetTagEventTest extends AbstractRemoteEntityEventTest<DistributionSetTag> {

    /**
     * Verifies that the distribution set tag entity reloading by remote created event works
     */
    @Test
    void testDistributionSetTagCreatedEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagCreatedEvent.class);
    }

    /**
     * Verifies that the distribution set tag entity reloading by remote updated event works
     */
    @Test
    void testDistributionSetTagUpdateEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagUpdatedEvent.class);
    }

    @Override
    protected DistributionSetTag createEntity() {
        return distributionSetTagManagement.create(entityFactory.tag().create().name("tag1"));
    }

}
