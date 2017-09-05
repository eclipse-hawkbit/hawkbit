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
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test DistributionSetCreatedEvent")
public class DistributionSetCreatedEventTest extends AbstractRemoteEntityEventTest<DistributionSet> {

    @Test
    @Description("Verifies that the distribution set entity reloading by remote created event works")
    public void testDistributionSetCreatedEvent() {
        assertAndCreateRemoteEvent(DistributionSetCreatedEvent.class);
    }

    @Override
    protected DistributionSet createEntity() {
        return distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("incomplete").version("2").description("incomplete").type("os"));
    }

}
