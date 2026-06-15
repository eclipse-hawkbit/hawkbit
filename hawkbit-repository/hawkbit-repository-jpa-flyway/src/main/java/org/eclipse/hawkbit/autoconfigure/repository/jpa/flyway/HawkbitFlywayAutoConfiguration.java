/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.repository.jpa.flyway;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * hawkBit Flyway autoconfiguration loading the flyway defaults properties.
 * <p/>
 * Used when this module is packed in a spring boot application in order to migrate the hawkbit jpa database schema at runtime.
 * Another option will be to use hawkbit-repository-jpa-init module to migrate the database schema before hawkbit is started.
 */
@Configuration
@PropertySource("classpath:/hawkbit-jpa-flyway-defaults.properties")
@AutoConfigureBefore(FlywayAutoConfiguration.class) // ensure that property source is loaded before FlywayAutoConfiguration
public class HawkbitFlywayAutoConfiguration {}