/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.amqp.AmqpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for controlling the device simulator.
 */
@RestController
public class SimulationController {

    @Autowired
    private DeviceSimulatorRepository repository;

    @Autowired
    private SimulatedDeviceFactory deviceFactory;

    @Autowired
    private AmqpProperties amqpProperties;

    /**
     * The start resource to start a device creation.
     * 
     * @param name
     *            the name prefix of the generated device naming
     * @param amount
     *            the amount of devices to be created
     * @param tenant
     *            the tenant to create the device to
     * @param api
     *            the api-protocol to be used either {@code dmf} or {@code ddi}
     * @param endpoint
     *            the URL endpoint to be used of the hawkbit-update-server for
     *            DDI devices
     * @param pollDelay
     *            number of delay in milliseconds to delay polling of DDI
     *            devices
     * @param gatewayToken
     *            the hawkbit-update-server gatewaytoken in case authentication
     *            is enforced in hawkbit
     * @return a response string that devices has been created
     * @throws MalformedURLException
     */
    @RequestMapping("/start")
    ResponseEntity<String> start(@RequestParam(value = "name", defaultValue = "simulated") final String name,
            @RequestParam(value = "amount", defaultValue = "20") final int amount,
            @RequestParam(value = "tenant", defaultValue = "DEFAULT") final String tenant,
            @RequestParam(value = "api", defaultValue = "dmf") final String api,
            @RequestParam(value = "endpoint", defaultValue = "http://localhost:8080") final String endpoint,
            @RequestParam(value = "polldelay", defaultValue = "1800") final int pollDelay,
            @RequestParam(value = "gatewaytoken", defaultValue = "") final String gatewayToken)
            throws MalformedURLException {

        final Protocol protocol;
        switch (api.toLowerCase()) {
        case "dmf":
            protocol = Protocol.DMF_AMQP;
            break;
        case "ddi":
            protocol = Protocol.DDI_HTTP;
            break;
        default:
            return ResponseEntity.badRequest().body("query param api only allows value of 'dmf' or 'ddi'");
        }

        if (protocol == Protocol.DMF_AMQP && isDmfDisabled()) {
            return ResponseEntity.badRequest()
                    .body("The AMQP interface has been disabled, to use DMF protocol you need to enable the AMQP interface via '"
                            + AmqpProperties.CONFIGURATION_PREFIX + ".enabled=true'");
        }

        for (int i = 0; i < amount; i++) {
            final String deviceId = name + i;
            repository.add(deviceFactory.createSimulatedDeviceWithImmediatePoll(deviceId, tenant, protocol, pollDelay,
                    new URL(endpoint), gatewayToken));
        }

        return ResponseEntity.ok("Updated " + amount + " " + protocol + " connected targets!");
    }

    private boolean isDmfDisabled() {
        return !amqpProperties.isEnabled();
    }
}
