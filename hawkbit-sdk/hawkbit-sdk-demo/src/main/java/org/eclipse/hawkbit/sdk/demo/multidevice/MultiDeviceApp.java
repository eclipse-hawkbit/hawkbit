/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.demo.multidevice;

import feign.Client;
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
import org.eclipse.hawkbit.sdk.mgmt.MgmtApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Abstract class representing DDI device connecting directly to hawkVit.
 */
@Slf4j
@SpringBootApplication
public class MultiDeviceApp {

    public static void main(String[] args) {
        SpringApplication.run(MultiDeviceApp.class, args);
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

    @Bean
    MgmtApi mgmtApi(final Tenant defaultTenant, final HawkbitClient hawkbitClient) {
        return new MgmtApi(defaultTenant, hawkbitClient);
    }

    @ShellComponent
    public static class Shell {

        private final DdiTenant ddiTenant;
        private final MgmtApi mgmtApi;
        private final UpdateHandler updateHandler;

        private boolean setup;

        Shell(final DdiTenant ddiTenant, final MgmtApi mgmtApi, final Optional<UpdateHandler> updateHandler) {
            this.ddiTenant = ddiTenant;
            this.mgmtApi = mgmtApi;
            this.updateHandler = updateHandler.orElse(null);
        }

        @ShellMethod(key = "setup")
        public void setup() {
            mgmtApi.setupTargetAuthentication();
            setup = true;
        }

        @ShellMethod(key = "start-one")
        public void startOne(@ShellOption("--id") final String controllerId) {
            final String securityTargetToken;
            if (setup) {
                securityTargetToken = mgmtApi.setupTargetToken(controllerId,null);
            } else {
                securityTargetToken = null;
            }
            // Create device with security token if not yet registered in this execution
            // if already created in this execution of app, just start the poll
            // for each new device - separate ThreadScheduler
            ddiTenant.getController(controllerId).ifPresentOrElse(
                    ddiController -> ddiController.start(Executors.newSingleThreadScheduledExecutor()),
                    () -> ddiTenant.createController(Controller.builder()
                                .controllerId(controllerId)
                                .securityToken(securityTargetToken)
                                .build(),updateHandler)
                            .setOverridePollMillis(10_000)
                            .start(Executors.newSingleThreadScheduledExecutor())
                    );
        }

        @ShellMethod(key = "stop-one")
        public void stopOne(@ShellOption("--id") final String controllerId) {
            ddiTenant.getController(controllerId).ifPresentOrElse(
                    DdiController::stop,
                    () -> log.error("Controller with id " + controllerId + " not found!"));

        }

        @ShellMethod(key = "start")
        public void start(
                @ShellOption(value = "--prefix", defaultValue = "") final String prefix,
                @ShellOption(value = "--offset", defaultValue = "0") final int offset,
                @ShellOption(value = "--count") final int count) {
            for (int i = 0; i < count; i++) {
                startOne(toId(prefix, offset + i));
            }
        }

        @ShellMethod(key = "stop")
        public void stop(
                @ShellOption(value = "--prefix", defaultValue = "") final String prefix,
                @ShellOption(value = "--offset", defaultValue = "0") final int offset,
                @ShellOption(value = "--count") final int count) {
            for (int i = 0; i < count; i++) {
                stopOne(toId(prefix, offset + i));
            }
        }

        private static String toId(final String prefix, final int index) {
            return String.format("%s%03d", prefix, index);
        }
    }
}