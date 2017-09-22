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
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test RolloutUpdatedEvent")
public class RolloutEventTest extends AbstractRemoteEntityEventTest<Rollout> {

    @Test
    @Description("Verifies that the rollout entity reloading by remote updated event works")
    public void testRolloutUpdatedEvent() {
        assertAndCreateRemoteEvent(RolloutUpdatedEvent.class);
    }

    @Override
    protected Rollout createEntity() {
        testdataFactory.createTarget("12345");
        final DistributionSet ds = distributionSetManagement.create(entityFactory.distributionSet()
                .create().name("incomplete").version("2").description("incomplete").type("os"));

        return rolloutManagement.create(
                entityFactory.rollout().create().name("exampleRollout").targetFilterQuery("controllerId==*").set(ds),
                10, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());
    }

}
