/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.rest.OpenApi;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enable {@link ComponentScan} in the resource package to set up all {@link Controller} annotated classes and set up the REST-Resources
 * for the Management API.
 */
@Configuration
@EnableMethodSecurity(proxyTargetClass = true, securedEnabled = true)
@ComponentScan
@Import({ RestConfiguration.class, OpenApi.class })
@PropertySource("classpath:/hawkbit-mgmt-api-defaults.properties")
public class MgmtApiConfiguration implements WebMvcConfigurer {}