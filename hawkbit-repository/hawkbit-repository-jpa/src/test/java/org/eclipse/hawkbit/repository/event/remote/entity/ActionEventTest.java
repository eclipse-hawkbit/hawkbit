/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test ActionCreatedEvent and ActionUpdatedEvent")
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
    protected Action createEntity() {
        final JpaAction generateAction = (JpaAction) entityFactory.generateAction();
        generateAction.setActionType(ActionType.FORCED);
        final Target generateTarget = entityFactory.generateTarget("Test");
        final Target target = targetManagement.createTarget(generateTarget);
        generateAction.setTarget(target);
        return actionRepository.save(generateAction);
    }

}
