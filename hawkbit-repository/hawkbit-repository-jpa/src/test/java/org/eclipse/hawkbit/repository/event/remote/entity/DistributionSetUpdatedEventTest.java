/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test DistributionSetUpdateEvent")
public class DistributionSetUpdatedEventTest extends AbstractRemoteEntityEventTest<DistributionSet> {

    @Test
    @Description("Verifies that the distribution set entity reloading by remote updated event works")
    public void testDistributionSetUpdateEvent() {
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
