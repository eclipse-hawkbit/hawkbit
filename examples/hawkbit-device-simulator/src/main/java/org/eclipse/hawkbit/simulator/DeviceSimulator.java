/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main-method to start the Spring-Boot application.
 *
 *
 *
 */
@SpringBootApplication
public class DeviceSimulator {

    private DeviceSimulator() {
        // utility class
    }

    /**
     * Start the Spring Boot Application.
     *
     * @param args
     *            the args
     */
    public static void main(final String[] args) {
        SpringApplication.run(DeviceSimulator.class, args);
    }
}
