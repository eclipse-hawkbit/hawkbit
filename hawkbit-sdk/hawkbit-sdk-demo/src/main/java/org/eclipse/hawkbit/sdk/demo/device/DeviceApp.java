/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.demo.device;

import feign.Client;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.device.SetupHelper;
import org.eclipse.hawkbit.sdk.device.DdiController;
import org.eclipse.hawkbit.sdk.device.DdiTenant;
import org.eclipse.hawkbit.sdk.device.UpdateHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.util.ObjectUtils;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract class representing DDI device connecting directly to hawkVit.
 */
@Slf4j
@SpringBootApplication
public class DeviceApp {

    public static void main(String[] args) {
        SpringApplication.run(DeviceApp.class, args);
    }

    @Bean
    HawkbitClient hawkbitClient(
            final HawkbitServer hawkBitServer,
            final Client client, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return new HawkbitClient(hawkBitServer, client, encoder, decoder, contract);
    }

    @Bean
    DdiTenant ddiTenant(final Tenant defaultTenant,
                        final HawkbitClient hawkbitClient) {
        return new DdiTenant(defaultTenant, hawkbitClient);
    }

    @ShellComponent
    public static class Shell {

        private final DdiTenant ddiTenant;

        private final DdiController device;

        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Shell(final DdiTenant ddiTenant, final Optional<UpdateHandler> updateHandler) {
            this.ddiTenant = ddiTenant;
            String controllerId = System.getProperty("demo.controller.id");
            String securityToken = System.getProperty("demo.controller.securityToken");

            this.device = this.ddiTenant.createController(Controller.builder()
                            .controllerId(controllerId)
                            .securityToken(ObjectUtils.isEmpty(securityToken) ?
                                    (ObjectUtils.isEmpty(ddiTenant.getTenant().getGatewayToken()) ? SetupHelper.randomToken() : securityToken) :
                                    securityToken)
                            .build(),
                    updateHandler.orElse(null)).setOverridePollMillis(10_000);
        }

        @ShellMethod(key = "setup")
        public void setup() {
            ddiTenant.resetTargetAuthentication();
            ddiTenant.registerOrUpdateToken(device.getControllerId(), device.getTargetSecurityToken());
        }

        @ShellMethod(key = "start")
        public void start() {
            device.start(scheduler);
        }

        @ShellMethod(key = "stop")
        public void stop() {
            device.stop();
        }
    }
}