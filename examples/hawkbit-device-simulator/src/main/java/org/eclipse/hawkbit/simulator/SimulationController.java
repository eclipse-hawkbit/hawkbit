/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.amqp.SpSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for controlling the device simulator.
 * 
 *
 *
 */
@RestController
public class SimulationController {

    @Autowired
    private SpSenderService spSenderService;

    @Autowired
    private DeviceSimulatorRepository repository;

    @Autowired
    private SimulatedDeviceFactory deviceFactory;

    /**
     * The start resource to start a device creation.
     * 
     * @param name
     *            the name prefix of the generated device naming
     * @param amount
     *            the amount of devices to be created
     * @param tenant
     *            the tenant to create the device to
     * @return a response string that devices has been created
     */
    @RequestMapping("/start")
    String start(@RequestParam(value = "name", defaultValue = "dmfSimulated") final String name,
            @RequestParam(value = "amount", defaultValue = "20") final int amount,
            @RequestParam(value = "tenant", defaultValue = "DEFAULT") final String tenant) {

        for (int i = 0; i < amount; i++) {
            final String deviceId = name + i;
            repository.add(deviceFactory.createSimulatedDevice(deviceId, tenant, Protocol.DMF_AMQP));
            spSenderService.createOrUpdateThing(tenant, deviceId);
        }

        return "Updated " + amount + " DMF connected targets!";
    }
}
