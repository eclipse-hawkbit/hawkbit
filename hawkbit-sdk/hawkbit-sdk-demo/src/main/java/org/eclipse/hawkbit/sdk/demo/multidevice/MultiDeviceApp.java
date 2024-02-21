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
import org.eclipse.hawkbit.sdk.HawkbitServer;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.HawkbitSDKConfigurtion;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.demo.SetupHelper;
import org.eclipse.hawkbit.sdk.device.DdiController;
import org.eclipse.hawkbit.sdk.device.UpdateHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

    @ShellComponent
    public static class Shell {

        private final Tenant tenant;
        private final UpdateHandler updateHandler;
        private final HawkbitClient hawkbitClient;
        private final Map<String, DdiController> devices = new ConcurrentHashMap<>();

        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        private boolean setup;

        Shell(final Tenant tenant, final Optional<UpdateHandler> updateHandler, final HawkbitClient hawkbitClient) {
            this.tenant = tenant;
            this.updateHandler = updateHandler.orElse(null);
            this.hawkbitClient = hawkbitClient;
        }

        @ShellMethod(key = "setup")
        public void setup() {
            SetupHelper.setupTargetAuthentication(hawkbitClient, tenant);
            setup = true;
        }

        @ShellMethod(key = "start-one")
        public void startOne(@ShellOption("--id") final String controllerId) {
            DdiController device = devices.get(controllerId);
            final String securityTargetToken;
            if (setup) {
                securityTargetToken = SetupHelper.setupTargetToken(
                        controllerId, null, hawkbitClient, tenant);
            } else {
                securityTargetToken = null;
            }
            if (device == null) {
                device = new DdiController(
                        tenant,
                        Controller.builder()
                                .controllerId(controllerId)
                                .securityToken(securityTargetToken)
                                .build(),
                        updateHandler,
                        hawkbitClient).setOverridePollMillis(10_000);
                final DdiController oldDevice = devices.putIfAbsent(controllerId, device);
                if (oldDevice != null) {
                    device = oldDevice; // reuse existing
                }
            }

            device.start(scheduler);
        }

        @ShellMethod(key = "stop-one")
        public void stopOne(@ShellOption("--id") final String controllerId) {
            final DdiController device = devices.get(controllerId);
            if (device == null) {
                throw new IllegalArgumentException("Controller with id " + controllerId + " not found!");
            } else {
                device.stop();
            }
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
