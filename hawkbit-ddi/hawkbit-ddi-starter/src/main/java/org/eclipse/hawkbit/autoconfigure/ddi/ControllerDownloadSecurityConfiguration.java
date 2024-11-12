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
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticateAnonymousDownloadFilter;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.autoconfigure.ddi.security.HttpControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.MdcHandler;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Security configuration for the hawkBit server DDI download interface.
 */
@Slf4j
@Configuration
class ControllerDownloadSecurityConfiguration {

    private static final String DDI_DL_ANT_MATCHER = DdiRestConstants.BASE_V1_REQUEST_MAPPING +
            "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/*";

    private final ControllerManagement controllerManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final TenantAware tenantAware;
    private final DdiSecurityProperties ddiSecurityConfiguration;
    private final HawkbitSecurityProperties securityProperties;
    private final SystemSecurityContext systemSecurityContext;

    @Autowired
    ControllerDownloadSecurityConfiguration(final ControllerManagement controllerManagement,
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
     * Filter to protect the hawkBit server DDI download interface against too many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosFilterDDIDL(final HawkbitSecurityProperties securityProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = SecurityManagedConfiguration.dosFilter(List.of(DDI_DL_ANT_MATCHER),
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setOrder(SecurityManagedConfiguration.DOS_FILTER_ORDER);
        filterRegBean.setName("dosDDiDlFilter");

        return filterRegBean;
    }

    @Bean
    @Order(301)
    protected SecurityFilterChain filterChainDDIDL(final HttpSecurity http) throws Exception {
        final AuthenticationManager authenticationManager = ControllerSecurityConfiguration.setAuthenticationManager(
                http, ddiSecurityConfiguration);

        http
                .securityMatcher(DDI_DL_ANT_MATCHER)
                .csrf(AbstractHttpConfigurer::disable);

        if (securityProperties.isRequireSsl()) {
            http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
        }

        final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

        if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {
            log.warn(
                    SecurityManagedConfiguration.ANONYMOUS_CONTROLLER_SECURITY_ENABLED_SHOULD_ONLY_BE_USED_FOR_DEVELOPMENT_PURPOSES);

            final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                    "controllerAnonymousFilter", "anonymous",
                    List.of(new SimpleGrantedAuthority(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
            anonymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);
            http
                    .securityContext(AbstractHttpConfigurer::disable)
                    .anonymous(configurer -> configurer.authenticationFilter(anonymousFilter));
        } else {
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

            final HttpControllerPreAuthenticateAnonymousDownloadFilter controllerAnonymousDownloadFilter = new HttpControllerPreAuthenticateAnonymousDownloadFilter(
                    tenantConfigurationManagement, tenantAware, systemSecurityContext);
            controllerAnonymousDownloadFilter.setAuthenticationManager(authenticationManager);
            controllerAnonymousDownloadFilter.setCheckForPrincipalChanges(true);
            controllerAnonymousDownloadFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            http
                    .authorizeHttpRequests(amrmRegistry -> amrmRegistry.anyRequest().authenticated())
                    .anonymous(AbstractHttpConfigurer::disable)
                    .addFilter(securityHeaderFilter)
                    .addFilter(securityTokenFilter)
                    .addFilter(gatewaySecurityTokenFilter)
                    .addFilter(controllerAnonymousDownloadFilter)
                    .exceptionHandling(configurer -> configurer.authenticationEntryPoint(
                            (request, response, authException) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                    .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }

        MdcHandler.Filter.addMdcFilter(http);

        return http.build();
    }
}