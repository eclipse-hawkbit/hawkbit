/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test DistributionSetTagCreatedEvent and DistributionSetTagUpdateEvent")
public class DistributionSetTagEventTest extends AbstractRemoteEntityEventTest<DistributionSetTag> {

    @Test
    @Description("Verifies that the distribution set tag entity reloading by remote created event works")
    public void testDistributionSetTagCreatedEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the distribution set tag entity reloading by remote updated event works")
    public void testDistributionSetTagUpdateEvent() {
        assertAndCreateRemoteEvent(DistributionSetTagUpdatedEvent.class);
    }

    @Override
    protected DistributionSetTag createEntity() {
        return distributionSetTagManagement.create(entityFactory.tag().create().name("tag1"));
    }

}
