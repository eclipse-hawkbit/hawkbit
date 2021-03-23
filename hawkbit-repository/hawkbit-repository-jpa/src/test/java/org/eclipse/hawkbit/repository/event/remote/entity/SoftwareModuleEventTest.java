/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test SoftwareModuleCreatedEvent, SoftwareModuleUpdatedEvent")
public class SoftwareModuleEventTest extends AbstractRemoteEntityEventTest<SoftwareModule> {

    @Test
    @Description("Verifies that the software module entity reloading by remote created event works")
    public void testTargetCreatedEvent() {
        assertAndCreateRemoteEvent(SoftwareModuleCreatedEvent.class);
    }

    @Test
    @Description("Verifies that the software module entity reloading by remote updated event works")
    public void testTargetUpdatedEvent() {
        assertAndCreateRemoteEvent(SoftwareModuleUpdatedEvent.class);
    }

    @Override
    protected SoftwareModule createEntity() {
        return testdataFactory.createSoftwareModuleApp();
    }

}
