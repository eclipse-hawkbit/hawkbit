/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import java.util.Collections;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test RolloutUpdatedEvent")
public class RolloutEventTest extends AbstractRemoteEntityEventTest<Rollout> {

    @Test
    @Description("Verifies that the rollout entity reloading by remote updated event works")
    public void testRolloutUpdatedEvent() {
        assertAndCreateRemoteEvent(RolloutUpdatedEvent.class);
    }

    @Override
    protected Rollout createEntity() {
        testdataFactory.createTarget("12345");
        final SoftwareModule module = softwareModuleManagement.create(
                entityFactory.softwareModule().create().name("swm").version("2").description("desc").type("os"));
        final DistributionSet ds = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("complete").version("2").description("complete")
                        .type("os").modules(Collections.singletonList(module.getId())));

        return rolloutManagement.create(
                entityFactory.rollout().create().name("exampleRollout").targetFilterQuery("controllerId==*").set(ds), 5,
                false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());
    }

}
