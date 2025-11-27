/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi;

import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.security.controller.DdiSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Auto-Configuration for enabling the DDI REST-Resources.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnClass(DdiApiConfiguration.class)
@Import({ DdiApiConfiguration.class, DdiSecurityProperties.class })
public class DdiApiAutoConfiguration {}