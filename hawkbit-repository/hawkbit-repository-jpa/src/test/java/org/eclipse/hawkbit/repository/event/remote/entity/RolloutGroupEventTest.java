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

import java.util.Set;
import java.util.UUID;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RolloutManagement.Create;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test RolloutGroupCreatedEvent and RolloutGroupUpdatedEvent
 */
class RolloutGroupEventTest extends AbstractRemoteEntityEventTest<RolloutGroup> {

    /**
     * Verifies that the rollout group entity reloading by remote created event works
     */
    @Test
    void testRolloutGroupCreatedEvent() {
        final RolloutGroupCreatedEvent createdEvent = (RolloutGroupCreatedEvent) assertAndCreateRemoteEvent(RolloutGroupCreatedEvent.class);
        assertThat(createdEvent.getRolloutId()).isNotNull();
    }

    /**
     * Verifies that the rollout group entity reloading by remote updated event works
     */
    @Test
    void testRolloutGroupUpdatedEvent() {
        assertAndCreateRemoteEvent(RolloutGroupUpdatedEvent.class);
    }

    @Override
    protected int getConstructorParamCount() {
        return 2;
    }

    @Override
    protected Object[] getConstructorParams(final RolloutGroup baseEntity) {
        return new Object[] { baseEntity, 1L };
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
                SoftwareModuleManagement.Create.builder()
                        .type(softwareModuleTypeManagement.findByKey("os").orElseThrow())
                        .name("swm").version("2").description("desc")
                        .build());
        final DistributionSet ds = distributionSetManagement
                .create(DistributionSetManagement.Create.builder()
                        .type(distributionSetTypeManagement.findByKey("os").orElseThrow())
                        .name("complete").version("2").description("complete")
                        .modules(Set.of(module))
                        .build());

        final Rollout entity = rolloutManagement.create(
                Create.builder().name("exampleRollout").targetFilterQuery("controllerId==*").distributionSet(ds).build(),
                5,
                false,
                new RolloutGroupConditionBuilder().withDefaults().successCondition(RolloutGroupSuccessCondition.THRESHOLD, "10").build());

        return rolloutGroupManagement.findByRollout(entity.getId(), PAGE).getContent().get(0);
    }
}