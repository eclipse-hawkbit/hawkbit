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

import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test TargetCreatedEvent, TargetUpdatedEvent and CancelTargetAssignmentEvent")
public class TargetEventTest extends AbstractRemoteEntityEventTest<Target> {

    @Test
    @Description("Verifies that the target entity reloading by remote created event works")
    public void testTargetCreatedEvent() {
        assertAndCreateRemoteEvent(TargetCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the target entity reloading by remote updated event works")
    public void testTargetUpdatedEvent() {
        assertAndCreateRemoteEvent(TargetUpdatedEvent.class);
    }

    @Test
    @Description("Verifies that cancel target assignment event works")
    public void testCancelTargetAssignmentEvent() {
        final Target target = createEntity();
        final CancelTargetAssignmentEvent assignmentEvent = new CancelTargetAssignmentEvent(target, 1L, "node");
        final CancelTargetAssignmentEvent underTest = (CancelTargetAssignmentEvent) assertEntity(target,
                assignmentEvent);

        assertThat(underTest.getActionId()).isNotNull();
    }

    @Override
    protected Target createEntity() {
        return testdataFactory.createTarget("12345");
    }

}
