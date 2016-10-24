/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.fest.assertions.Assertions.assertThat;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test TargetInfoUpdateEvent")
public class TargetInfoUpdateEventTest extends AbstractRemoteEntityEventTest<Target> {

    private TargetInfo targetInfo;

    @Test
    @Description("Verifies that the target tag entity reloading by remote created event works")
    public void testTargetInfoUpdateEvent() {
        final TargetInfoUpdateEvent underTest = (TargetInfoUpdateEvent) assertAndCreateRemoteEvent(
                TargetInfoUpdateEvent.class);

        assertThat(underTest.getEntity().getControllerId()).isEqualTo(targetInfo.getTarget().getControllerId());
        assertThat(underTest.getTargetInfo()).isEqualTo(targetInfo);
    }

    @Override
    protected Target createEntity() {
        final Target target = targetManagement.createTarget(entityFactory.generateTarget("test"));
        this.targetInfo = targetManagement.findTargetByControllerID(target.getControllerId()).getTargetInfo();
        return target;
    }

}
