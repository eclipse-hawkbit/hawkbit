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
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.EnableVaadin;

/**
 * The main-method to start the Spring-Boot application.
 *
 */
@SpringBootApplication
@EnableVaadin
@EnableScheduling
public class DeviceSimulator {

    public DeviceSimulator() {
        // utility class
    }

    /**
     * @return an asynchronous event bus to publish and retrieve events.
     */
    @Bean
    EventBus eventBus() {
        return new AsyncEventBus(Executors.newFixedThreadPool(4));
    }

    /**
     * @return central ScheduledExecutorService
     */
    @Bean
    ScheduledExecutorService threadPool() {
        return Executors.newScheduledThreadPool(8);
    }

    @Bean
    TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(threadPool());
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
