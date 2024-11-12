/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.dmf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A {@link SpringBootApplication} annotated class with a main method to start.
 * The minimal configuration for the stand alone hawkBit DMF server.
 */
@SpringBootApplication(scanBasePackages = "org.eclipse.hawkbit")
public class DMFStart {

    /**
     * Main method to start the spring-boot application.
     *
     * @param args the VM arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(DMFStart.class, args);
    }
}
