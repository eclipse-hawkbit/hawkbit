/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.eclipse.hawkbit.simulator.amqp.SpSenderService;

/**
 * A simulated device using the DMF API of the hawkBit update server.
 */
public class DMFSimulatedDevice extends AbstractSimulatedDevice {
    private final SpSenderService spSenderService;

    /**
     * @param id
     *            the ID of the device
     * @param tenant
     *            the tenant of the simulated device
     */
    public DMFSimulatedDevice(final String id, final String tenant, final SpSenderService spSenderService,
            final int pollDelaySec) {
        super(id, tenant, Protocol.DMF_AMQP, pollDelaySec);
        this.spSenderService = spSenderService;
    }

    @Override
    public void poll() {
        spSenderService.createOrUpdateThing(super.getTenant(), super.getId());
    }

}
