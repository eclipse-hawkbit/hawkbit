/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Default configuration that is common to all repository implementations.
 */
@Configuration
@EnableConfigurationProperties({ RepositoryProperties.class, ControllerPollProperties.class,
        TenantConfigurationProperties.class })
@PropertySource("classpath:/hawkbit-repository-defaults.properties")
public class RepositoryDefaultConfiguration {

}
