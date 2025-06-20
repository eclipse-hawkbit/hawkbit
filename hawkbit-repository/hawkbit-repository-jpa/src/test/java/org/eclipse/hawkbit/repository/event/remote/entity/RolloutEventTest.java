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

import java.util.Collections;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
  * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test RolloutUpdatedEvent
 */
class RolloutEventTest extends AbstractRemoteEntityEventTest<Rollout> {

    /**
     * Verifies that the rollout entity reloading by remote updated event works
     */
    @Test    void testRolloutUpdatedEvent() {
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
                entityFactory.rollout().create().name("exampleRollout").targetFilterQuery("controllerId==*").distributionSetId(ds), 5,
                false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());
    }
}