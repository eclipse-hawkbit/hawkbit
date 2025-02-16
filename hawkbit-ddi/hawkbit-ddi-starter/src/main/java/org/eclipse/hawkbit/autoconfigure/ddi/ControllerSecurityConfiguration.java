/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.ddi;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.autoconfigure.ddi.security.ControllerTenantAwareAuthenticationDetailsSource;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.MdcHandler;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.controller.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the hawkBit server DDI interface.
 */
@Slf4j
@Configuration
@EnableWebSecurity
class ControllerSecurityConfiguration {

    private static final String[] DDI_ANT_MATCHERS = { DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/**" };

    private final ControllerManagement controllerManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final TenantAware tenantAware;
    private final DdiSecurityProperties ddiSecurityConfiguration;
    private final HawkbitSecurityProperties securityProperties;
    private final SystemSecurityContext systemSecurityContext;

    @Autowired
    ControllerSecurityConfiguration(final ControllerManagement controllerManagement,
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final DdiSecurityProperties ddiSecurityConfiguration,
            final HawkbitSecurityProperties securityProperties, final SystemSecurityContext systemSecurityContext) {
        this.controllerManagement = controllerManagement;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.tenantAware = tenantAware;
        this.ddiSecurityConfiguration = ddiSecurityConfiguration;
        this.securityProperties = securityProperties;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Filter to protect the hawkBit server DDI interface against too many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    protected FilterRegistrationBean<DosFilter> dosFilterDDI(final HawkbitSecurityProperties securityProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean =
                SecurityManagedConfiguration.dosFilter(List.of(DDI_ANT_MATCHERS),
                        securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setOrder(SecurityManagedConfiguration.DOS_FILTER_ORDER);
        filterRegBean.setName("dosDDiFilter");

        return filterRegBean;
    }

    @Bean
    @Order(301)
    protected SecurityFilterChain filterChainDDI(final HttpSecurity http) throws Exception {
        final AuthenticationManager authenticationManager = setAuthenticationManager(http, ddiSecurityConfiguration);

        http
                .securityMatcher(DDI_ANT_MATCHERS)
                .csrf(AbstractHttpConfigurer::disable);

        if (securityProperties.isRequireSsl()) {
            http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
        }

        final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

        final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                ddiSecurityConfiguration.getRp().getCnHeader(),
                ddiSecurityConfiguration.getRp().getSslIssuerHashHeader(), tenantConfigurationManagement,
                tenantAware, systemSecurityContext);
        securityHeaderFilter.setAuthenticationManager(authenticationManager);
        securityHeaderFilter.setCheckForPrincipalChanges(true);
        securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

        final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new HttpControllerPreAuthenticateSecurityTokenFilter(
                tenantConfigurationManagement, tenantAware, controllerManagement, systemSecurityContext);
        securityTokenFilter.setAuthenticationManager(authenticationManager);
        securityTokenFilter.setCheckForPrincipalChanges(true);
        securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

        final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                tenantConfigurationManagement, tenantAware, systemSecurityContext);
        gatewaySecurityTokenFilter.setAuthenticationManager(authenticationManager);
        gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
        gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

        http
                .authorizeHttpRequests(amrmRegistry -> amrmRegistry.anyRequest().authenticated())
                .anonymous(AbstractHttpConfigurer::disable)
                .addFilter(securityHeaderFilter)
                .addFilter(securityTokenFilter)
                .addFilter(gatewaySecurityTokenFilter)
                .exceptionHandling(configurer -> configurer.authenticationEntryPoint(
                        (request, response, authException) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        MdcHandler.Filter.addMdcFilter(http);

        return http.build();
    }

    static AuthenticationManager setAuthenticationManager(final HttpSecurity http, final DdiSecurityProperties ddiSecurityConfiguration)
            throws Exception {
        // configure authentication manager
        final AuthenticationManager authenticationManager =
                http
                        .getSharedObject(AuthenticationManagerBuilder.class)
                        .authenticationProvider(
                                new PreAuthTokenSourceTrustAuthenticationProvider(ddiSecurityConfiguration.getRp().getTrustedIPs()))
                        .build();
        http.authenticationManager(authenticationManager);
        return authenticationManager;
    }
}