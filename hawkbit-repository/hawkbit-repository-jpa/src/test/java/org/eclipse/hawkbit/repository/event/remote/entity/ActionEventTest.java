/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test ActionCreatedEvent and ActionUpdatedEvent")
public class ActionEventTest extends AbstractRemoteEntityEventTest<Action> {

    @Test
    @Description("Verifies that the action entity reloading by remote created works")
    public void testActionCreatedEvent() {
        assertAndCreateRemoteEvent(ActionCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the action entity reloading by remote updated works")
    public void testActionUpdatedEvent() {
        assertAndCreateRemoteEvent(ActionUpdatedEvent.class);
    }

    @Override
    protected int getConstructorParamCount() {
        return 5;
    }

    @Override
    protected Object[] getConstructorParams(final Action baseEntity) {
        return new Object[] { baseEntity, 1L, 1L, 2L, "Node" };
    }

    @Override
    protected RemoteEntityEvent<?> assertEntity(final Action baseEntity, final RemoteEntityEvent<?> e) {
        final AbstractActionEvent event = (AbstractActionEvent) e;

        assertThat(event.getEntity()).isSameAs(baseEntity);
        assertThat(event.getTargetId()).isEqualTo(1L);
        assertThat(event.getRolloutId()).isEqualTo(1L);
        assertThat(event.getRolloutGroupId()).isEqualTo(2L);

        AbstractActionEvent underTestCreatedEvent = createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getTargetId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);
        assertThat(underTestCreatedEvent.getRolloutGroupId()).isEqualTo(2L);

        underTestCreatedEvent = createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
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
        generateAction.setInitiatedBy(tenantAware.getCurrentUsername());
        return actionRepository.save(generateAction);
    }

}
