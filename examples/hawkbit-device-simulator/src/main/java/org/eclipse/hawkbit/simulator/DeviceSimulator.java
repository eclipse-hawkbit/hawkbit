/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.concurrent.Executors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.EnableVaadin;

/**
 * The main-method to start the Spring-Boot application.
 *
 */
@SpringBootApplication
@EnableVaadin
public class DeviceSimulator {

    public DeviceSimulator() {
        // utility class
    }

    /**
     * @return an asynchronous event bus to publish and retrieve events.
     */
    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(Executors.newFixedThreadPool(4));
    }

    /**
     * Start the Spring Boot Application.
     *
     * @param args
     *            the args
     */
    // Exception squid:S2095 - Spring boot standard behavior
    @SuppressWarnings({ "squid:S2095" })
    public static void main(final String[] args) {
        SpringApplication.run(DeviceSimulator.class, args);
    }
}
