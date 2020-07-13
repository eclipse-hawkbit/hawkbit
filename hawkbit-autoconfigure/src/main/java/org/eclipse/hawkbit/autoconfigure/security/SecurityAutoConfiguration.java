/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableConfigurationProperties({ DdiSecurityProperties.class, HawkbitSecurityProperties.class })
public class SecurityAutoConfiguration {

    /**
     * @return the {@link TenantAware} singleton bean which holds the current
     *         {@link TenantAware} service and make it accessible in beans which
     *         cannot access the service directly, e.g. JPA entities.
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantAware tenantAware() {
        return new SecurityContextTenantAware();
    }

    /**
     * @return permission service to check if current user has the necessary
     *         permissions.
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionService permissionService() {
        return new PermissionService();
    }

    /**
     * Creates the auditor aware.
     * 
     * @return the spring security auditor aware
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * @param tenantAware
     *            singleton bean
     * @return tenantAware {@link SystemSecurityContext}
     */
    @Bean
    @ConditionalOnMissingBean
    public SystemSecurityContext systemSecurityContext(final TenantAware tenantAware) {
        return new SystemSecurityContext(tenantAware);
    }

    /**
     * @return {@link SecurityTokenGenerator} bean
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityTokenGenerator securityTokenGenerator() {
        return new SecurityTokenGenerator();
    }

    /**
     * @return {@link AuthenticationSuccessHandler} bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler();
    }

    /**
     * @return {@link LogoutHandler} bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LogoutHandler logoutHandler() {
        return new SecurityContextLogoutHandler();
    }

    /**
     * @return {@link LogoutSuccessHandler} bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LogoutSuccessHandler logoutSuccessHandler() {
        final SimpleUrlLogoutSuccessHandler simpleUrlLogoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        simpleUrlLogoutSuccessHandler.setTargetUrlParameter("login");
        return simpleUrlLogoutSuccessHandler;
    }

}
