/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.eclipse.hawkbit.auth.StaticAuthenticationProvider;
import org.eclipse.hawkbit.tenancy.TenantAwareUserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

/**
 * Autoconfiguration for the in-memory-user-management.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({ TenantAwareUserProperties.class })
public class StaticUserManagementAutoConfiguration extends GlobalAuthenticationConfigurerAdapter {

    private final StaticAuthenticationProvider authenticationProvider;

    StaticUserManagementAutoConfiguration(
            final SecurityProperties securityProperties,
            final TenantAwareUserProperties tenantAwareUserProperties) {
        authenticationProvider = new StaticAuthenticationProvider(tenantAwareUserProperties, securityProperties);
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
    }
}