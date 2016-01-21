/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.http.ControllerResource;
import org.eclipse.hawkbit.simulator.http.GatewayTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import feign.Feign;
import feign.Logger;

/**
 * The simulated device factory to create either {@link DMFSimulatedDevice} or
 * {@link DDISimulatedDevice#}.
 * 
 * @author Michael Hirsch
 *
 */
@Service
public class SimulatedDeviceFactory {

    private static final ScheduledExecutorService pollThreadPool = Executors.newScheduledThreadPool(4);

    @Autowired
    private DeviceSimulatorUpdater deviceUpdater;

    /**
     * Creating a simulated devices.
     * 
     * @param id
     *            the ID of the simulated device
     * @param tenant
     *            the tenant of the simulated device
     * @param protocol
     *            the protocol of the device
     * @return the created simulated device
     */
    public AbstractSimulatedDevice createSimulatedDevice(final String id, final String tenant, final Protocol protocol) {
        return createSimulatedDevice(id, tenant, protocol, 30, null, null);
    }

    /**
     * Creating a simulated device.
     * 
     * @param id
     *            the ID of the simulated device
     * @param tenant
     *            the tenant of the simulated device
     * @param protocol
     *            the protocol which should be used be the simulated device
     * @param pollDelaySec
     *            the poll delay time in seconds which should be used for
     *            {@link DDISimulatedDevice}s
     * @param baseEndpoint
     *            the http base endpoint which should be used for
     *            {@link DDISimulatedDevice}s
     * @param gatewayToken
     *            the gatewayToken to be used to authenticate
     *            {@link DDISimulatedDevice}s at the endpoint
     * @return the created simulated device
     */
    public AbstractSimulatedDevice createSimulatedDevice(final String id, final String tenant, final Protocol protocol,
            final int pollDelaySec, final URL baseEndpoint, final String gatewayToken) {
        switch (protocol) {
        case DMF_AMQP:
            return new DMFSimulatedDevice(id, tenant);
        case DDI_HTTP:
            final ControllerResource controllerResource = Feign.builder().logger(new Logger.ErrorLogger())
                    .requestInterceptor(new GatewayTokenInterceptor(gatewayToken)).logLevel(Logger.Level.BASIC)
                    .target(ControllerResource.class, baseEndpoint.toString());
            return new DDISimulatedDevice(id, tenant, pollDelaySec, controllerResource, pollThreadPool, deviceUpdater);
        default:
            throw new IllegalArgumentException("Protocol " + protocol + " unknown");
        }
    }
}
