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

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Test ActionCreatedEvent and ActionUpdatedEvent
 */
class ActionEventTest extends AbstractRemoteEntityEventTest<Action> {

    /**
     * Verifies that the action entity reloading by remote created works
     */
    @Test
    void testActionCreatedEvent() {
        assertAndCreateRemoteEvent(ActionCreatedEvent.class);
    }

    /**
     * Verifies that the action entity reloading by remote updated works
     */
    @Test
    void testActionUpdatedEvent() {
        assertAndCreateRemoteEvent(ActionUpdatedEvent.class);
    }

    @Override
    protected int getConstructorParamCount() {
        return 4;
    }

    @Override
    protected Object[] getConstructorParams(final Action baseEntity) {
        return new Object[] { baseEntity, 1L, 1L, 2L };
    }

    @Override
    protected RemoteEntityEvent<?> assertEntity(final Action baseEntity, final RemoteEntityEvent<?> e) {
        final AbstractActionEvent event = (AbstractActionEvent) e;

        assertThat(event.getEntity()).hasValue(baseEntity);
        assertThat(event.getTargetId()).isEqualTo(1L);
        assertThat(event.getRolloutId()).isEqualTo(1L);
        assertThat(event.getRolloutGroupId()).isEqualTo(2L);

        AbstractActionEvent underTestCreatedEvent = createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).hasValue(baseEntity);
        assertThat(underTestCreatedEvent.getTargetId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutGroupId()).isEqualTo(2L);

        underTestCreatedEvent = createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).hasValue(baseEntity);
        assertThat(underTestCreatedEvent.getTargetId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutGroupId()).isEqualTo(2L);

        return underTestCreatedEvent;
    }

    @Override
    protected Action createEntity() {
        final JpaAction generateAction = new JpaAction();
        generateAction.setActionType(ActionType.FORCED);
        final Target target = testdataFactory.createTarget("Test");
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        generateAction.setTarget(target);
        generateAction.setDistributionSet(distributionSet);
        generateAction.setStatus(Status.RUNNING);
        generateAction.setInitiatedBy(AccessContext.actor());
        generateAction.setWeight(1000);
        return actionRepository.save(generateAction);
    }
}