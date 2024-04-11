/**
 * Copyright (c) 2024 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.sdk.demo.dmf;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.sdk.Controller;
import org.eclipse.hawkbit.sdk.Tenant;
import org.eclipse.hawkbit.sdk.dmf.DmfController;
import org.eclipse.hawkbit.sdk.dmf.DmfTenant;
import org.eclipse.hawkbit.sdk.dmf.UpdateHandler;
import org.eclipse.hawkbit.sdk.dmf.amqp.Amqp;
import org.eclipse.hawkbit.sdk.dmf.amqp.AmqpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Optional;

/**
 * Abstract class representing DDI device connecting directly to hawkVit.
 */
@Slf4j
@EnableConfigurationProperties({ RabbitProperties.class, AmqpProperties.class })
@SpringBootApplication
public class DmfApp {

    public static void main(String[] args) {
        SpringApplication.run(DmfApp.class, args);
    }

    @Bean
    Amqp amqp(final RabbitProperties rabbitProperties, final AmqpProperties amqpProperties) {
        return new Amqp(rabbitProperties, amqpProperties);
    }

    @ShellComponent
    public static class Shell {

        private final UpdateHandler updateHandler;
        private final DmfTenant dmfTenant;

        Shell(final Tenant tenant, final Optional<UpdateHandler> updateHandler, final Amqp amqp) {
            this.updateHandler = updateHandler.orElse(null);
            dmfTenant = new DmfTenant(tenant, amqp);
        }

        @ShellMethod(key = "start-one")
        public void startOne(@ShellOption("--id") final String controllerId) {
            if (dmfTenant.getController(controllerId).isEmpty()) {
                dmfTenant.create(
                        Controller.builder().controllerId(controllerId).build(),
                        updateHandler).connect();
            }
        }

        @ShellMethod(key = "stop-one")
        public void stopOne(@ShellOption("--id") final String controllerId) {
            dmfTenant.getController(controllerId).ifPresentOrElse(
                    DmfController::stop,
                    () -> {
                        throw new IllegalArgumentException("Controller with id " + controllerId + " not found!");
                    });
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