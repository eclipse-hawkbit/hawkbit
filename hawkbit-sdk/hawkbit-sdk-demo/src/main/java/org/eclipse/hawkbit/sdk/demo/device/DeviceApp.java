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

import java.util.Optional;
import java.util.concurrent.Executors;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.device.DdiController;
import org.eclipse.hawkbit.sdk.device.DdiTenant;
import org.eclipse.hawkbit.sdk.device.UpdateHandler;
import org.eclipse.hawkbit.sdk.mgmt.AuthenticationSetupHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
    HawkbitClient hawkbitClient(final HawkbitServer hawkBitServer, final Encoder encoder, final Decoder decoder, final Contract contract) {
        return new HawkbitClient(hawkBitServer, encoder, decoder, contract);
    }

    @Bean
    DdiTenant ddiTenant(final Tenant defaultTenant, final HawkbitClient hawkbitClient) {
        return new DdiTenant(defaultTenant, hawkbitClient);
    }

    @Bean
    AuthenticationSetupHelper mgmtApi(final Tenant tenant, final HawkbitClient hawkbitClient) {
        return new AuthenticationSetupHelper(tenant, hawkbitClient);
    }

    @Component
    public static class Shell {

        private final DdiTenant ddiTenant;

        private final DdiController device;
        private final AuthenticationSetupHelper mgmtApi;

        @SuppressWarnings("java:S3358")
        Shell(final DdiTenant ddiTenant, final AuthenticationSetupHelper mgmtApi, final Optional<UpdateHandler> updateHandler,
                @Value("${demo.controller.id:demo}") final String controllerId,
                @Value("${demo.controller.securityToken:#{null}") final String securityToken) {
            this.ddiTenant = ddiTenant;
            this.mgmtApi = mgmtApi;

            this.device = this.ddiTenant.createController(
                    Controller.builder()
                            .controllerId(controllerId)
                            .securityToken(ObjectUtils.isEmpty(securityToken)
                                    ? (ObjectUtils.isEmpty(ddiTenant.getTenant().getGatewayToken())
                                    ? AuthenticationSetupHelper.randomToken()
                                    : securityToken)
                                    : securityToken)
                            .build(),
                    updateHandler.orElse(null)).setOverridePollMillis(10_000);
        }

        @Command(name = "setup")
        public void setup() {
            mgmtApi.setupTargetAuthentication();
            mgmtApi.setupTargetSecureToken(device.getController().getControllerId(), device.getTargetSecurityToken());
        }

        @Command(name = "start")
        public void start() {
            device.start(Executors.newSingleThreadScheduledExecutor());
        }

        @Command(name = "stop")
        public void stop() {
            device.stop();
        }
    }
}