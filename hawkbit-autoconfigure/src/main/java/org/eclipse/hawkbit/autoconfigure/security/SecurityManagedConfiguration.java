/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.ddi.rest.api.DdiRestConstants;
import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
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
import org.eclipse.hawkbit.ui.MgmtUiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

/**
 * All configurations related to HawkBit's authentication and authorization
 * layer.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, mode = AdviceMode.ASPECTJ, proxyTargetClass = true, securedEnabled = true)
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:/hawkbit-security-defaults.properties")
public class SecurityManagedConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityManagedConfiguration.class);

    private static final int DOS_FILTER_ORDER = -200;

    /**
     * @return the {@link UserAuthenticationFilter} to include into the hawkBit
     *         security configuration.
     * @throws Exception
     *             lazy bean exception maybe if the authentication manager
     *             cannot be instantiated
     */
    @Bean
    @ConditionalOnMissingBean
    // Exception squid:S00112 - Is aspectJ proxy
    @SuppressWarnings({ "squid:S00112" })
    UserAuthenticationFilter userAuthenticationFilter(final AuthenticationConfiguration configuration)
            throws Exception {
        return new UserAuthenticationFilterBasicAuth(configuration.getAuthenticationManager());
    }

    private static final class UserAuthenticationFilterBasicAuth extends BasicAuthenticationFilter
            implements UserAuthenticationFilter {

        private UserAuthenticationFilterBasicAuth(final AuthenticationManager authenticationManager) {
            super(authenticationManager);
        }

    }

    /**
     * {@link WebSecurityConfigurer} for the hawkBit server DDI interface.
     */
    @Configuration
    @Order(300)
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private static final String[] DDI_ANT_MATCHERS = { DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/deploymentBase/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/cancelAction/**",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/configData",
                DdiRestConstants.BASE_V1_REQUEST_MAPPING
                        + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts" };

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
         * Filter to protect the hawkBit server DDI interface against to many
         * requests.
         *
         * @param securityProperties
         *            for filter configuration
         *
         * @return the spring filter registration bean for registering a denial
         *         of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosDDiFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Arrays.asList(DDI_ANT_MATCHERS),
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiFilter");

            return filterRegBean;
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

            final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                    ddiSecurityConfiguration.getRp().getCnHeader(),
                    ddiSecurityConfiguration.getRp().getSslIssuerHashHeader(), tenantConfigurationManagement,
                    tenantAware, systemSecurityContext);
            securityHeaderFilter.setAuthenticationManager(authenticationManager());
            securityHeaderFilter.setCheckForPrincipalChanges(true);
            securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new HttpControllerPreAuthenticateSecurityTokenFilter(
                    tenantConfigurationManagement, tenantAware, controllerManagement, systemSecurityContext);
            securityTokenFilter.setAuthenticationManager(authenticationManager());
            securityTokenFilter.setCheckForPrincipalChanges(true);
            securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                    tenantConfigurationManagement, tenantAware, systemSecurityContext);
            gatewaySecurityTokenFilter.setAuthenticationManager(authenticationManager());
            gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
            gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            HttpSecurity httpSec = http.csrf().disable();

            if (securityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            }

            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {

                LOG.info(
                        "******************\n** Anonymous controller security enabled, should only be used for developing purposes **\n******************");

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        Arrays.asList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
                anonymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);
                httpSec.requestMatchers().antMatchers(DDI_ANT_MATCHERS).and().securityContext().disable().anonymous()
                        .authenticationFilter(anonymousFilter);
            } else {

                httpSec.addFilter(securityHeaderFilter).addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter).requestMatchers().antMatchers(DDI_ANT_MATCHERS).and()
                        .anonymous().disable().authorizeRequests().anyRequest().authenticated().and()
                        .exceptionHandling()
                        .authenticationEntryPoint((request, response, authException) -> response
                                .setStatus(HttpStatus.UNAUTHORIZED.value()))
                        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

            auth.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));
        }
    }

    /**
     * {@link WebSecurityConfigurer} for the hawkBit server DDI download
     * interface.
     */
    @Configuration
    @Order(301)
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerDownloadSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

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
         * Filter to protect the hawkBit server DDI download interface against
         * to many requests.
         *
         * @param securityProperties
         *            for filter configuration
         *
         * @return the spring filter registration bean for registering a denial
         *         of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosDDiDlFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Arrays.asList(DDI_DL_ANT_MATCHER),
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiDlFilter");

            return filterRegBean;
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

            final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                    ddiSecurityConfiguration.getRp().getCnHeader(),
                    ddiSecurityConfiguration.getRp().getSslIssuerHashHeader(), tenantConfigurationManagement,
                    tenantAware, systemSecurityContext);
            securityHeaderFilter.setAuthenticationManager(authenticationManager());
            securityHeaderFilter.setCheckForPrincipalChanges(true);
            securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new HttpControllerPreAuthenticateSecurityTokenFilter(
                    tenantConfigurationManagement, tenantAware, controllerManagement, systemSecurityContext);
            securityTokenFilter.setAuthenticationManager(authenticationManager());
            securityTokenFilter.setCheckForPrincipalChanges(true);
            securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                    tenantConfigurationManagement, tenantAware, systemSecurityContext);
            gatewaySecurityTokenFilter.setAuthenticationManager(authenticationManager());
            gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
            gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateAnonymousDownloadFilter controllerAnonymousDownloadFilter = new HttpControllerPreAuthenticateAnonymousDownloadFilter(
                    tenantConfigurationManagement, tenantAware, systemSecurityContext);
            controllerAnonymousDownloadFilter.setAuthenticationManager(authenticationManager());
            controllerAnonymousDownloadFilter.setCheckForPrincipalChanges(true);
            controllerAnonymousDownloadFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            HttpSecurity httpSec = http.csrf().disable();

            if (securityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            }

            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {

                LOG.info(
                        "******************\n** Anonymous controller security enabled, should only be used for developing purposes **\n******************");

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        Arrays.asList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
                anonymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);
                httpSec.requestMatchers().antMatchers(DDI_DL_ANT_MATCHER).and().securityContext().disable().anonymous()
                        .authenticationFilter(anonymousFilter);
            } else {

                httpSec.addFilter(securityHeaderFilter).addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter).addFilter(controllerAnonymousDownloadFilter)
                        .requestMatchers().antMatchers(DDI_DL_ANT_MATCHER).and().anonymous().disable()
                        .authorizeRequests().anyRequest().authenticated().and().exceptionHandling()
                        .authenticationEntryPoint((request, response, authException) -> response
                                .setStatus(HttpStatus.UNAUTHORIZED.value()))
                        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {

            auth.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));
        }
    }

    /**
     * Filter to protect the hawkBit server system management interface against
     * to many requests.
     *
     * @param securityProperties
     *            for filter configuration
     *
     * @return the spring filter registration bean for registering a denial of
     *         service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosSystemFilter(final HawkbitSecurityProperties securityProperties) {

        final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Collections.emptyList(),
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setUrlPatterns(Arrays.asList("/system/*"));
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
     * A Websecurity config to handle and filter the download ids.
     */
    @Configuration
    @EnableWebSecurity
    @Order(320)
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class IdRestSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private DdiSecurityProperties ddiSecurityConfiguration;

        @Autowired
        private DownloadIdCache downloadIdCache;

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            final HttpDownloadAuthenticationFilter downloadIdAuthenticationFilter = new HttpDownloadAuthenticationFilter(
                    downloadIdCache);
            downloadIdAuthenticationFilter.setAuthenticationManager(authenticationManager());

            http.csrf().disable();
            http.anonymous().disable();

            http.regexMatcher(HttpDownloadAuthenticationFilter.REQUEST_ID_REGEX_PATTERN)
                    .addFilterBefore(downloadIdAuthenticationFilter, FilterSecurityInterceptor.class);
            http.authorizeRequests().anyRequest().authenticated().and().sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));
        }
    }

    /**
     * Security configuration for the REST management API.
     */
    @Configuration
    @Order(350)
    @EnableWebSecurity
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class RestSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private UserAuthenticationFilter userAuthenticationFilter;

        @Autowired(required = false)
        private OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter;

        @Autowired(required = false)
        private InMemoryClientRegistrationRepository clientRegistrationRepository;

        @Autowired
        private SystemManagement systemManagement;

        @Autowired
        private HawkbitSecurityProperties securityProperties;

        @Autowired
        private SystemSecurityContext systemSecurityContext;

        /**
         * Filter to protect the hawkBit server Management interface against to
         * many requests.
         *
         * @param securityProperties
         *            for filter configuration
         *
         * @return the spring filter registration bean for registering a denial
         *         of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosMgmtFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(null,
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setUrlPatterns(Arrays.asList("/rest/*", "/api/*"));
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosMgmtFilter");

            return filterRegBean;
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            HttpSecurity httpSec = http.regexMatcher("\\/rest.*|\\/system/admin.*").csrf().disable();

            if (securityProperties.getCors().isEnabled()) {
                httpSec = httpSec.cors().and();
            }

            if (securityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            }

            httpSec.authorizeRequests().antMatchers(MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                    .hasAnyAuthority(SpPermission.SYSTEM_ADMIN).anyRequest().authenticated();

            if (oidcBearerTokenAuthenticationFilter != null) {

                // Only get the first client registration. Testing against every
                // client could increase the
                // attack vector
                final ClientRegistration clientRegistration = clientRegistrationRepository != null
                        && clientRegistrationRepository.iterator().hasNext()
                                ? clientRegistrationRepository.iterator().next()
                                : null;

                Assert.notNull(clientRegistration, "There must be a valid client registration");
                httpSec.oauth2ResourceServer().jwt().jwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri());

                oidcBearerTokenAuthenticationFilter.setClientRegistration(clientRegistration);

                httpSec.addFilterAfter(oidcBearerTokenAuthenticationFilter, BearerTokenAuthenticationFilter.class);
            } else {
                final BasicAuthenticationEntryPoint basicAuthEntryPoint = new BasicAuthenticationEntryPoint();
                basicAuthEntryPoint.setRealmName(securityProperties.getBasicRealm());

                httpSec.addFilterBefore(new Filter() {
                    @Override
                    public void init(final FilterConfig filterConfig) throws ServletException {
                        userAuthenticationFilter.init(filterConfig);
                    }

                    @Override
                    public void doFilter(final ServletRequest request, final ServletResponse response,
                            final FilterChain chain) throws IOException, ServletException {
                        userAuthenticationFilter.doFilter(request, response, chain);
                    }

                    @Override
                    public void destroy() {
                        userAuthenticationFilter.destroy();
                    }
                }, RequestHeaderAuthenticationFilter.class);
                httpSec.httpBasic().and().exceptionHandling().authenticationEntryPoint(basicAuthEntryPoint);
            }

            httpSec.addFilterAfter(
                    new AuthenticationSuccessTenantMetadataCreationFilter(systemManagement, systemSecurityContext),
                    SessionManagementFilter.class);

            httpSec.anonymous().disable();
            httpSec.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.cors", name = "enabled", matchIfMissing = false)
        CorsConfigurationSource corsConfigurationSource() {
            final CorsConfiguration restCorsConfiguration = new CorsConfiguration();

            restCorsConfiguration.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
            restCorsConfiguration.setAllowCredentials(true);
            restCorsConfiguration.setAllowedHeaders(securityProperties.getCors().getAllowedHeaders());
            restCorsConfiguration.setAllowedMethods(securityProperties.getCors().getAllowedMethods());

            final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/rest/**", restCorsConfiguration);

            return source;
        }
    }

    /**
     * {@link WebSecurityConfigurer} for external (management) access.
     */
    @Configuration
    @Order(400)
    @EnableWebSecurity
    @EnableVaadinSharedSecurity
    @ConditionalOnClass(MgmtUiConfiguration.class)
    public static class UISecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private HawkbitSecurityProperties hawkbitSecurityProperties;

        @Autowired(required = false)
        private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

        @Autowired(required = false)
        private AuthenticationSuccessHandler authenticationSuccessHandler;

        @Autowired
        private LogoutHandler logoutHandler;

        @Autowired
        private LogoutSuccessHandler logoutSuccessHandler;

        /**
         * Filter to protect the hawkBit management UI against to many requests.
         *
         * @param securityProperties
         *            for filter configuration
         *
         * @return the spring filter registration bean for registering a denial
         *         of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.ui-filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosMgmtUiFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(null,
                    securityProperties.getDos().getUiFilter(), securityProperties.getClients());
            // All URLs that can be called anonymous
            filterRegBean.setUrlPatterns(Arrays.asList("/UI/login", "/UI/login/*", "/UI/logout", "/UI/logout/*"));
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosMgmtUiFilter");

            return filterRegBean;
        }

        @Override
        @Bean(name = VaadinSharedSecurityConfiguration.AUTHENTICATION_MANAGER_BEAN)
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        /**
         * Overwriting VaadinAuthenticationSuccessHandler of default VaadinSharedSecurityConfiguration
         * @return the vaadin success authentication handler
         */
        @Primary
        @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_AUTHENTICATION_SUCCESS_HANDLER_BEAN)
        public VaadinAuthenticationSuccessHandler redirectSaveHandler(final HttpService httpService,
                final VaadinRedirectStrategy redirectStrategy) {
            final VaadinUrlAuthenticationSuccessHandler handler = new TenantMetadataSavedRequestAwareVaadinAuthenticationSuccessHandler(
                    httpService, redirectStrategy, "/UI/");
            handler.setTargetUrlParameter("r");

            return handler;
        }

        /**
         * Listener to redirect to login page after session timeout. Close the
         * vaadin session, because it's is not possible to redirect in
         * atmosphere.
         *
         * @return the servlet listener.
         */
        @Bean
        public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
            return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            final boolean enableOidc = oidcUserService != null && authenticationSuccessHandler != null;

            // workaround regex: we need to exclude the URL /UI/HEARTBEAT here
            // because we bound the vaadin application to /UI and not to root,
            // described in vaadin-forum:
            // https://vaadin.com/forum#!/thread/3200565.
            HttpSecurity httpSec;
            if (enableOidc) {
                httpSec = http.regexMatcher("(?!.*HEARTBEAT)^.*\\/(UI|oauth2).*$");
            } else {
                httpSec = http.regexMatcher("(?!.*HEARTBEAT)^.*\\/UI.*$");
            }
            // disable as CSRF is handled by Vaadin
            httpSec.csrf().disable();

            if (hawkbitSecurityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            } else {

                LOG.info(
                        "\"******************\\n** Requires HTTPS Security has been disabled for UI, should only be used for developing purposes **\\n******************\"");
            }

            if (!StringUtils.isEmpty(hawkbitSecurityProperties.getContentSecurityPolicy())) {
                httpSec.headers().contentSecurityPolicy(hawkbitSecurityProperties.getContentSecurityPolicy());
            }

            // UI
            httpSec.authorizeRequests().antMatchers("/UI/login/**", "/UI/UIDL/**").permitAll().anyRequest()
                    .authenticated();

            if (enableOidc) {
                // OIDC
                httpSec.oauth2Login().userInfoEndpoint().oidcUserService(oidcUserService).and()
                        .successHandler(authenticationSuccessHandler).and().oauth2Client();
            } else {
                // UI login / Basic auth
                httpSec.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/UI/login"));
            }

            // UI logout
            httpSec.logout().logoutUrl("/UI/logout*").addLogoutHandler(logoutHandler)
                    .logoutSuccessHandler(logoutSuccessHandler);
        }

        /**
         * HttpFirewall which enables to define a list of allowed host names.
         *
         * @return the http firewall.
         */
        @Bean
        public HttpFirewall httpFirewall() {
            final List<String> allowedHostNames = hawkbitSecurityProperties.getAllowedHostNames();
            final IgnorePathsStrictHttpFirewall firewall = new IgnorePathsStrictHttpFirewall(
                    hawkbitSecurityProperties.getHttpFirewallIgnoredPaths());

            if (!CollectionUtils.isEmpty(allowedHostNames)) {
                firewall.setAllowedHostnames(hostName -> {
                    LOG.debug("Firewall check host: {}, allowed: {}", hostName, allowedHostNames.contains(hostName));
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
                            //nothing to do
                        }
                    };
                }
                return super.getFirewalledRequest(request);
            }
        }

        @Override
        public void configure(final WebSecurity webSecurity) throws Exception {
            // No security for static content
            webSecurity.ignoring().antMatchers("/documentation/**", "/VAADIN/**", "/*.*", "/docs/**");
        }

        /**
         * Configuration that defines the {@link AccessDecisionManager} bean for
         * UI method security used by the Vaadin Servlet. Notice: we can not use
         * the top-level method security configuration because
         * {@link AdviceMode.ASPECTJ} is not supported.
         */
        @Configuration
        @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
        @ConditionalOnClass(MgmtUiConfiguration.class)
        static class UIMethodSecurity extends GlobalMethodSecurityConfiguration {

            @Bean(name = VaadinSharedSecurityConfiguration.ACCESS_DECISION_MANAGER_BEAN)
            @Override
            protected AccessDecisionManager accessDecisionManager() {
                return super.accessDecisionManager();
            }
        }
    }
}

/**
 * After a successful login on the UI we need to ensure to create the tenant
 * meta data within SP.
 */
class TenantMetadataSavedRequestAwareVaadinAuthenticationSuccessHandler extends VaadinUrlAuthenticationSuccessHandler {

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    public TenantMetadataSavedRequestAwareVaadinAuthenticationSuccessHandler(final HttpService http,
            final VaadinRedirectStrategy redirectStrategy, final String defaultTargetUrl) {
        super(http, redirectStrategy, defaultTargetUrl);
    }

    @Override
    public void onAuthenticationSuccess(final Authentication authentication) throws Exception {
        systemSecurityContext.runAsSystemAsTenant(systemManagement::getTenantMetadata, getTenantFrom(authentication));

        super.onAuthenticationSuccess(authentication);
    }

    private static String getTenantFrom(final Authentication authentication) {
        final Object details = authentication.getDetails();
        if (details instanceof TenantAwareAuthenticationDetails) {
            return ((TenantAwareAuthenticationDetails) details).getTenant();
        }

        throw new InsufficientAuthenticationException("Authentication details/tenant info are not specified!");
    }
}

/**
 * Servletfilter to create metadata after successful authentication over
 * RESTful.
 */
class AuthenticationSuccessTenantMetadataCreationFilter implements Filter {

    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;

    AuthenticationSuccessTenantMetadataCreationFilter(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not needed
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        lazyCreateTenantMetadata();
        chain.doFilter(request, response);

    }

    private void lazyCreateTenantMetadata() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            systemSecurityContext.runAsSystem(systemManagement::getTenantMetadata);
        }
    }

    @Override
    public void destroy() {
        // not needed
    }

}
