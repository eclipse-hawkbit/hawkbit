/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * General simulator service properties.
 *
 */
@Component
@ConfigurationProperties("hawkbit.device.simulator")
public class SimulationProperties {

    /**
     * List of tenants where the simulator should auto start simulations after
     * startup.
     */
    private final List<Autostart> autostarts = new ArrayList<>();

    public List<Autostart> getAutostarts() {
        return this.autostarts;
    }

    /**
     * Auto start configuration for simulation setups that the simulator begins
     * after startup.
     *
     */
    public static class Autostart {
        /**
         * Name prefix of simulated devices, followed by counter, e.g.
         * simulated0, simulated1, simulated2....
         */
        private String name = "simulated";

        /**
         * Amount of simulated devices.
         */
        private int amount = 20;

        /**
         * Tenant name for the simulation.
         */
        @NotEmpty
        private String tenant;

        /**
         * API for simulation.
         */
        private Protocol api = Protocol.DMF_AMQP;

        /**
         * Endpoint in case of DDI API based simulation.
         */
        private String endpoint = "http://localhost:8080";

        /**
         * Poll time in {@link TimeUnit#SECONDS} for simulated devices.
         */
        private int pollDelay = (int) TimeUnit.MINUTES.toSeconds(30);

        /**
         * Optional gateway token for DDI API based simulation.
         */
        private String gatewayToken = "";

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(final int amount) {
            this.amount = amount;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(final String tenant) {
            this.tenant = tenant;
        }

        public Protocol getApi() {
            return api;
        }

        public void setApi(final Protocol api) {
            this.api = api;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(final String endpoint) {
            this.endpoint = endpoint;
        }

        public int getPollDelay() {
            return pollDelay;
        }

        public void setPollDelay(final int pollDelay) {
            this.pollDelay = pollDelay;
        }

        public String getGatewayToken() {
            return gatewayToken;
        }

        public void setGatewayToken(final String gatewayToken) {
            this.gatewayToken = gatewayToken;
        }
    }
}
