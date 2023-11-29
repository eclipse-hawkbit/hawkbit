/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.doc;

import org.eclipse.hawkbit.autoconfigure.security.EnableHawkbitManagedSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A {@link SpringBootApplication} annotated class with a main method to start.
 * The minimal configuration for the stand alone hawkBit server.
 *
 */
@SpringBootApplication
@EnableHawkbitManagedSecurityConfiguration
// Exception squid:S1118 - Spring boot standard behavior
@SuppressWarnings({ "squid:S1118" })
public class Start {

    /**
     * Main method to start the spring-boot application.
     *
     * @param args
     *            the VM arguments.
     */
    // Exception squid:S2095 - Spring boot standard behavior
    @SuppressWarnings({ "squid:S2095" })
    public static void main(final String[] args) {
        SpringApplication.run(Start.class, args);
    }
}
