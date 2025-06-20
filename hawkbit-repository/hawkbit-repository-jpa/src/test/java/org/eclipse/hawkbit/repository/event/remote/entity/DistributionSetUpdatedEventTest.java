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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
  * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test DistributionSetUpdateEvent
 */
class DistributionSetUpdatedEventTest extends AbstractRemoteEntityEventTest<DistributionSet> {

    /**
     * Verifies that the distribution set entity reloading by remote updated event works
     */
    @Test
    void testDistributionSetUpdateEvent() {
        assertAndCreateRemoteEvent(DistributionSetUpdatedEvent.class);
    }

    @Override
    protected RemoteEntityEvent<?> createRemoteEvent(final DistributionSet baseEntity,
            final Class<? extends RemoteEntityEvent<?>> eventType) {

        return new DistributionSetUpdatedEvent(baseEntity, "1", true);
    }

    @Override
    protected DistributionSet createEntity() {
        return distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("incomplete").version("2").description("incomplete").type("os"));
    }

}
