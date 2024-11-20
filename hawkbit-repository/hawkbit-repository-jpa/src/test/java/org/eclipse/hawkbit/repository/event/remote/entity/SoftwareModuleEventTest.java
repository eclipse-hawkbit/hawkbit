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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

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
