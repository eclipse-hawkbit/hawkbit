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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test DistributionSetCreatedEvent")
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
