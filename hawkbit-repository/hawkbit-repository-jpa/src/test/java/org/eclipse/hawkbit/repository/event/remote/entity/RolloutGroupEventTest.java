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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.UUID;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
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
@Story("Test RolloutGroupCreatedEvent and RolloutGroupUpdatedEvent")
public class RolloutGroupEventTest extends AbstractRemoteEntityEventTest<RolloutGroup> {

    @Test
    @Description("Verifies that the rollout group entity reloading by remote created event works")
    public void testRolloutGroupCreatedEvent() {
        final RolloutGroupCreatedEvent createdEvent = (RolloutGroupCreatedEvent) assertAndCreateRemoteEvent(
                RolloutGroupCreatedEvent.class);
        assertThat(createdEvent.getRolloutId()).isNotNull();
    }

    @Test
    @Description("Verifies that the rollout group entity reloading by remote updated event works")
    public void testRolloutGroupUpdatedEvent() {
        assertAndCreateRemoteEvent(RolloutGroupUpdatedEvent.class);
    }

    @Override
    protected int getConstructorParamCount() {
        return 3;
    }

    @Override
    protected Object[] getConstructorParams(final RolloutGroup baseEntity) {
        return new Object[] { baseEntity, 1L, "Node" };
    }

    @Override
    protected RemoteEntityEvent<?> assertEntity(final RolloutGroup baseEntity, final RemoteEntityEvent<?> e) {
        final AbstractRolloutGroupEvent event = (AbstractRolloutGroupEvent) e;

        assertThat(event.getEntity()).isPresent().get().isSameAs(baseEntity);
        assertThat(event.getRolloutId()).isEqualTo(1L);

        AbstractRolloutGroupEvent underTestCreatedEvent = createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);

        underTestCreatedEvent = createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);

        return underTestCreatedEvent;
    }

    @Override
    protected RolloutGroup createEntity() {
        testdataFactory.createTarget(UUID.randomUUID().toString());
        final SoftwareModule module = softwareModuleManagement.create(
                entityFactory.softwareModule().create().name("swm").version("2").description("desc").type("os"));
        final DistributionSet ds = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("complete").version("2").description("complete")
                        .type("os").modules(Collections.singletonList(module.getId())));

        final Rollout entity = rolloutManagement.create(
                entityFactory.rollout().create().name("exampleRollout").targetFilterQuery("controllerId==*").distributionSetId(ds), 5,
                false, new RolloutGroupConditionBuilder().withDefaults()
                        .successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());

        return rolloutGroupManagement.findByRollout(PAGE, entity.getId()).getContent().get(0);
    }
}