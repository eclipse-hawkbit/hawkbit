/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Test TargetTagCreatedEvent and TargetTagUpdateEvent")
public class TargetTagEventTest extends AbstractRemoteEntityEventTest<TargetTag> {

    @Test
    @Description("Verifies that the target tag entity reloading by remote created event works")
    public void testTargetTagCreatedEvent() {
        assertAndCreateRemoteEvent(TargetTagCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the target tag entity reloading by remote updated event works")
    public void testTargetTagUpdateEventt() {
        assertAndCreateRemoteEvent(TargetTagUpdatedEvent.class);
    }

    @Override
    protected TargetTag createEntity() {
        return targetTagManagement.create(entityFactory.tag().create().name("tag1"));
    }

}
