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

import org.eclipse.hawkbit.audit.AuditContextProvider;
import org.eclipse.hawkbit.audit.AuditLoggingAspect;
import org.eclipse.hawkbit.audit.HawkbitAuditorAware;
import org.eclipse.hawkbit.audit.MdcHandler;
import org.eclipse.hawkbit.repository.RepositoryConfiguration;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.tenancy.TenantAwareUserProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security.
 */
@Configuration
@EnableConfigurationProperties({ SecurityProperties.class, HawkbitSecurityProperties.class, TenantAwareUserProperties.class })
@Import(RepositoryConfiguration.class)
public class SecurityAutoConfiguration {

    /**
     * Creates the auditor aware.
     *
     * @return the spring security auditor aware
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        return new HawkbitAuditorAware();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditContextProvider auditContextProvider() {
        return AuditContextProvider.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLoggingAspect auditLoggingAspect() {
        return new AuditLoggingAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public MdcHandler mdcHandler() {
        return MdcHandler.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityTokenGenerator securityTokenGenerator() {
        return new SecurityTokenGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogoutHandler logoutHandler() {
        return new SecurityContextLogoutHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogoutSuccessHandler logoutSuccessHandler() {
        final SimpleUrlLogoutSuccessHandler simpleUrlLogoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        simpleUrlLogoutSuccessHandler.setTargetUrlParameter("login");
        return simpleUrlLogoutSuccessHandler;
    }
}