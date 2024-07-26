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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.ControllerTenantAwareAuthenticationDetailsSource;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticateAnonymousDownloadFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.security.HttpDownloadAuthenticationFilter;
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * All configurations related to HawkBit's authentication and authorization
 * layer.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, mode = AdviceMode.ASPECTJ, proxyTargetClass = true, securedEnabled = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:hawkbit-security-defaults.properties")
public class SecurityManagedConfiguration {

    private static final int DOS_FILTER_ORDER = -200;
    public static final String ANONYMOUS_CONTROLLER_SECURITY_ENABLED_SHOULD_ONLY_BE_USED_FOR_DEVELOPMENT_PURPOSES = """
            ******************
            ** Anonymous controller security enabled, should only be used for development purposes **
            ******************""";

    /**
     * Security configuration for the hawkBit server DDI interface.
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerSecurityConfigurationAdapter {

        private static final String[] DDI_ANT_MATCHERS = {
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/confirmationBase/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/deploymentBase/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/installedBase/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/cancelAction/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/configData",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts" };

        private final ControllerManagement controllerManagement;
        private final TenantConfigurationManagement tenantConfigurationManagement;
        private final TenantAware tenantAware;
        private final DdiSecurityProperties ddiSecurityConfiguration;
        private final HawkbitSecurityProperties securityProperties;
        private final SystemSecurityContext systemSecurityContext;

        @Autowired
        ControllerSecurityConfigurationAdapter(final ControllerManagement controllerManagement,
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
                    dosFilter(List.of(DDI_ANT_MATCHERS),
                        securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiFilter");

            return filterRegBean;
        }

        @Bean
        @Order(300)
        protected SecurityFilterChain filterChainDDI(final HttpSecurity http) throws Exception {
            final AuthenticationManager authenticationManager = setAuthenticationManager(http, ddiSecurityConfiguration);

            http
                    .securityMatcher(DDI_ANT_MATCHERS)
                    .csrf(AbstractHttpConfigurer::disable);

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
            }

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();
            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {
                log.warn(ANONYMOUS_CONTROLLER_SECURITY_ENABLED_SHOULD_ONLY_BE_USED_FOR_DEVELOPMENT_PURPOSES);

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        List.of(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
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

                http
                        .authorizeHttpRequests(amrmRegistry ->
                                amrmRegistry.anyRequest().authenticated())
                        .anonymous(AbstractHttpConfigurer::disable)
                        .addFilter(securityHeaderFilter)
                        .addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter)
                        .exceptionHandling(configurer -> configurer.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                        .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            }

            return http.build();
        }
    }

    /**
     * Security configuration for the hawkBit server DDI download interface.
     */
    @Configuration
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerDownloadSecurityConfigurationAdapter {

        private static final String DDI_DL_ANT_MATCHER = DdiRestConstants.BASE_V1_REQUEST_MAPPING
                + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/*";

        private final ControllerManagement controllerManagement;
        private final TenantConfigurationManagement tenantConfigurationManagement;
        private final TenantAware tenantAware;
        private final DdiSecurityProperties ddiSecurityConfiguration;
        private final HawkbitSecurityProperties securityProperties;
        private final SystemSecurityContext systemSecurityContext;

        @Autowired
        ControllerDownloadSecurityConfigurationAdapter(final ControllerManagement controllerManagement,
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
            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(List.of(DDI_DL_ANT_MATCHER),
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiDlFilter");

            return filterRegBean;
        }

        @Bean
        @Order(301)
        protected SecurityFilterChain filterChainDDIDL(final HttpSecurity http) throws Exception {
            final AuthenticationManager authenticationManager = setAuthenticationManager(http, ddiSecurityConfiguration);

            http
                    .securityMatcher(DDI_DL_ANT_MATCHER)
                    .csrf(AbstractHttpConfigurer::disable);

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
            }

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {
                log.warn(ANONYMOUS_CONTROLLER_SECURITY_ENABLED_SHOULD_ONLY_BE_USED_FOR_DEVELOPMENT_PURPOSES);

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        List.of(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
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

            return http.build();
        }
    }

    /**
     * Filter to protect the hawkBit server system management interface against too many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosSystemFilter(final HawkbitSecurityProperties securityProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Collections.emptyList(),
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setUrlPatterns(List.of("/system/*"));
        filterRegBean.setOrder(DOS_FILTER_ORDER);
        filterRegBean.setName("dosSystemFilter");

        return filterRegBean;
    }

    private static FilterRegistrationBean<DosFilter> dosFilter(final Collection<String> includeAntPaths,
            final HawkbitSecurityProperties.Dos.Filter filterProperties,
            final HawkbitSecurityProperties.Clients clientProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = new FilterRegistrationBean<>();

        filterRegBean.setFilter(new DosFilter(includeAntPaths, filterProperties.getMaxRead(),
                filterProperties.getMaxWrite(), filterProperties.getWhitelist(), clientProperties.getBlacklist(),
                clientProperties.getRemoteIpHeader()));

        return filterRegBean;
    }

    /**
     * Security config to handle and filter the download ids.
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class IdRestSecurityConfigurationAdapter {

        @Bean
        @Order(320)
        protected SecurityFilterChain filterChainDLID(
                final HttpSecurity http,
                final DdiSecurityProperties ddiSecurityConfiguration, final DownloadIdCache downloadIdCache)
                throws Exception {
            final AuthenticationManager authenticationManager = setAuthenticationManager(http, ddiSecurityConfiguration);

            final HttpDownloadAuthenticationFilter downloadIdAuthenticationFilter = new HttpDownloadAuthenticationFilter(
                    downloadIdCache);
            downloadIdAuthenticationFilter.setAuthenticationManager(authenticationManager);

            http
                    .securityMatcher(MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + "/downloadId/*/*")
                    .authorizeHttpRequests(armrRepository -> armrRepository.anyRequest().authenticated())
                    .csrf(AbstractHttpConfigurer::disable)
                    .anonymous(AbstractHttpConfigurer::disable)
                    .addFilterBefore(downloadIdAuthenticationFilter, AuthorizationFilter.class)
                    .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();
        }
    }

    /**
     * Security configuration for the REST management API.
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class RestSecurityConfigurationAdapter {

        private final HawkbitSecurityProperties securityProperties;

        public RestSecurityConfigurationAdapter(final HawkbitSecurityProperties securityProperties) {
            this.securityProperties = securityProperties;
        }

        /**
         * Filter to protect the hawkBit server Management interface against to
         * many requests.
         *
         * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosFilterREST() {
            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(null,
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setUrlPatterns(List.of(
                    MgmtRestConstants.BASE_REST_MAPPING + "/*",
                    MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/*",
                    MgmtRestConstants.DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE + "/*"));
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosMgmtFilter");

            return filterRegBean;
        }

        @Bean
        @Order(350)
        SecurityFilterChain filterChainREST(
                final HttpSecurity http,
                @Autowired(required = false)
                @Qualifier("hawkbitOAuth2ResourceServerCustomizer")
                final Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer,
                // called just before build of the SecurityFilterChain.
                // could be used for instance to set authentication provider
                // Note: implementation of the customizer shall always take in account what is the already set by the
                //       hawkBit
                @Autowired(required = false)
                @Qualifier("hawkbitHttpSecurityCustomizer")
                final Customizer<HttpSecurity> httpSecurityCustomizer,
                final SystemManagement systemManagement,
                final SystemSecurityContext systemSecurityContext) throws Exception {
            http
                    .securityMatcher(MgmtRestConstants.BASE_REST_MAPPING + "/**", MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                    .authorizeHttpRequests(amrmRegistry ->
                            amrmRegistry
                                    .requestMatchers(MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                                        .hasAnyAuthority(SpPermission.SYSTEM_ADMIN)
                                    .anyRequest()
                                        .authenticated())
                    .anonymous(AbstractHttpConfigurer::disable)
                    .csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(Customizer.withDefaults())
                    .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterAfter(
                            // Servlet filter to create metadata after successful authentication over RESTful.
                            (request, response, chain) -> {
                                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                                if (authentication != null && authentication.isAuthenticated()) {
                                    systemSecurityContext.runAsSystem(systemManagement::getTenantMetadata);
                                }
                                chain.doFilter(request, response);
                            },
                            SessionManagementFilter.class);

            if (securityProperties.getCors().isEnabled()) {
                http.cors(configurer -> configurer.configurationSource(corsConfigurationSource()));
            }

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
            }

            if (oauth2ResourceServerCustomizer != null) {
                http.oauth2ResourceServer(oauth2ResourceServerCustomizer);
            }
            if (oauth2ResourceServerCustomizer == null || securityProperties.isAllowHttpBasicOnOAuthEnabled()) {
                http.httpBasic(configurer -> {
                    final BasicAuthenticationEntryPoint basicAuthEntryPoint = new BasicAuthenticationEntryPoint();
                    basicAuthEntryPoint.setRealmName(securityProperties.getBasicRealm());
                    configurer.authenticationEntryPoint(basicAuthEntryPoint);
                });
            }

            if (httpSecurityCustomizer != null) {
                httpSecurityCustomizer.customize(http);
            }

            return http.build();
        }

        private CorsConfigurationSource corsConfigurationSource() {
            final CorsConfiguration corsConfiguration = new CorsConfiguration();

            corsConfiguration.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedHeaders(securityProperties.getCors().getAllowedHeaders());
            corsConfiguration.setAllowedMethods(securityProperties.getCors().getAllowedMethods());
            corsConfiguration.setExposedHeaders(securityProperties.getCors().getExposedHeaders());
            return request -> corsConfiguration;
        }
    }


    /**
     * HttpFirewall which enables to define a list of allowed host names.
     *
     * @return the http firewall.
     */
    @Bean
    public HttpFirewall httpFirewall(final HawkbitSecurityProperties hawkbitSecurityProperties) {
        final List<String> allowedHostNames = hawkbitSecurityProperties.getAllowedHostNames();
        final IgnorePathsStrictHttpFirewall firewall = new IgnorePathsStrictHttpFirewall(
                hawkbitSecurityProperties.getHttpFirewallIgnoredPaths());

        if (!CollectionUtils.isEmpty(allowedHostNames)) {
            firewall.setAllowedHostnames(hostName -> {
                log.debug("Firewall check host: {}, allowed: {}", hostName, allowedHostNames.contains(hostName));
                return allowedHostNames.contains(hostName);
            });
        }
        return firewall;
    }


    private static class IgnorePathsStrictHttpFirewall extends StrictHttpFirewall {

        private final Collection<String> pathsToIgnore;

        public IgnorePathsStrictHttpFirewall(final Collection<String> pathsToIgnore) {
            super();
            this.pathsToIgnore = pathsToIgnore;
        }

        @Override
        public FirewalledRequest getFirewalledRequest(final HttpServletRequest request) {
            if (pathsToIgnore != null && pathsToIgnore.contains(request.getRequestURI())) {
                return new FirewalledRequest(request) {
                    @Override
                    public void reset() {
                        // nothing to do
                    }
                };
            }
            return super.getFirewalledRequest(request);
        }
    }

    private static AuthenticationManager setAuthenticationManager(final HttpSecurity http, final DdiSecurityProperties ddiSecurityConfiguration) throws Exception {
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
