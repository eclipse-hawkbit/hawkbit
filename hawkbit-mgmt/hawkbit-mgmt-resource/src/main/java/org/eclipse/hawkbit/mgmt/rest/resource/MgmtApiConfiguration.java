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

import org.eclipse.hawkbit.rest.OpenApiConfiguration;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;

/**
 * Enable {@link ComponentScan} in the resource package to setup all
 * {@link Controller} annotated classes and setup the REST-Resources for the
 * Management API.
 */
@Configuration
@ComponentScan
@Import({ RestConfiguration.class, OpenApiConfiguration.class })
@PropertySource("classpath:/hawkbit-mgmt-api-defaults.properties")
public class MgmtApiConfiguration {}
