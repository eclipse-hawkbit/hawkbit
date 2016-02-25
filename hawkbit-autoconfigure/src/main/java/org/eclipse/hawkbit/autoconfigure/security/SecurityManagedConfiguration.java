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
import java.net.URI;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.ExcludePathAwareShallowETagFilter;
import org.eclipse.hawkbit.cache.CacheConstants;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantUserPasswordAuthenticationToken;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.rest.resource.RestConstants;
import org.eclipse.hawkbit.security.ControllerTenantAwareAuthenticationDetailsSource;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.security.HttpDownloadAuthenticationFilter;
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.StaticAllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.vaadin.spring.security.VaadinSecurityContext;
import org.vaadin.spring.security.annotation.EnableVaadinSecurity;
import org.vaadin.spring.security.web.VaadinDefaultRedirectStrategy;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;
import org.vaadin.spring.security.web.authentication.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.authentication.VaadinUrlAuthenticationSuccessHandler;

/**
 * All configurations related to SP authentication and authorization layer.
 *
 *
 *
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, mode = AdviceMode.ASPECTJ, proxyTargetClass = true, securedEnabled = true)
@EnableWebMvcSecurity
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class SecurityManagedConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityManagedConfiguration.class);

    @Autowired
    private HawkbitSecurityProperties securityProperties;

    /**
     * {@link WebSecurityConfigurer} for the internal SP controller API.
     *
     *
     *
     */
    @Configuration
    @Order(300)
    static class ControllerSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private ControllerManagement controllerManagement;
        @Autowired
        private SystemManagement systemManagement;
        @Autowired
        private TenantAware tenantAware;
        @Autowired
        private DdiSecurityProperties securityConfiguration;
        @Autowired
        private org.springframework.boot.autoconfigure.security.SecurityProperties springSecurityProperties;

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource = new ControllerTenantAwareAuthenticationDetailsSource();

            final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter = new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                    securityConfiguration.getRp().getCnHeader(), securityConfiguration.getRp().getSslIssuerHashHeader(),
                    systemManagement, tenantAware);
            securityHeaderFilter.setAuthenticationManager(authenticationManager());
            securityHeaderFilter.setCheckForPrincipalChanges(true);
            securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter = new HttpControllerPreAuthenticateSecurityTokenFilter(
                    systemManagement, tenantAware, controllerManagement);
            securityTokenFilter.setAuthenticationManager(authenticationManager());
            securityTokenFilter.setCheckForPrincipalChanges(true);
            securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter = new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                    systemManagement, tenantAware);
            gatewaySecurityTokenFilter.setAuthenticationManager(authenticationManager());
            gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
            gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            HttpSecurity httpSec = http.csrf().disable().headers()
                    .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY)).contentTypeOptions()
                    .xssProtection().httpStrictTransportSecurity().and();

            if (springSecurityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            }

            if (securityConfiguration.getAuthentication().getAnonymous().isEnabled()) {
                LOG.info(
                        "******************\n** Anonymous controller security enabled, should only use for developing purposes **\n******************");
                final AnonymousAuthenticationFilter anoymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous", Collections.singletonList(
                                new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
                anoymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);
                httpSec.requestMatchers().antMatchers("/*/controller/v1/**", "/*/controller/artifacts/v1/**").and()
                        .securityContext().disable().anonymous().authenticationFilter(anoymousFilter);
            } else {
                httpSec.addFilter(securityHeaderFilter).addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter).antMatcher("/*/controller/**").anonymous().disable()
                        .authorizeRequests().anyRequest().authenticated().and().exceptionHandling()
                        .authenticationEntryPoint((request, response, authException) -> response
                                .setStatus(HttpStatus.UNAUTHORIZED.value()))
                        .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.springframework.security.config.annotation.web.configuration.
         * WebSecurityConfigurerAdapter
         * #configure(org.springframework.security.config.annotation.
         * authentication.builders. AuthenticationManagerBuilder)
         */
        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(
                    new PreAuthTokenSourceTrustAuthenticationProvider(securityConfiguration.getRp().getTrustedIPs()));
        }
    }

    /**
     * Filter to protect the SP server against denial of service attacks.
     *
     * @return he spring filter registration bean for registering an denial of
     *         service protection filter in the filter chain
     */
    @Bean
    @Order(50)
    public FilterRegistrationBean dosFilter() {
        final FilterRegistrationBean filterRegBean = new FilterRegistrationBean();

        filterRegBean.setFilter(new DosFilter(securityProperties.getDos().getFilter().getMaxRead(),
                securityProperties.getDos().getFilter().getMaxWrite(),
                securityProperties.getDos().getFilter().getWhitelist(), securityProperties.getClients().getBlacklist(),
                securityProperties.getClients().getRemoteIpHeader()));
        filterRegBean.addUrlPatterns("/{tenant}/controller/v1/*", "/rest/*");
        return filterRegBean;
    }

    /**
     * Filter registration bean for spring etag filter.
     *
     * @return the spring filter registration bean for registering an etag
     *         filter in the filter chain
     */
    @Bean
    @Order(100)
    public FilterRegistrationBean eTagFilter() {
        final FilterRegistrationBean filterRegBean = new FilterRegistrationBean();
        // eclude the URLs for downloading artifacts, so no eTag is generated in
        // the
        // ShallowEtagHeaderFilter, just using the SH1 hash of the artifact
        // itself as 'ETag', because
        // otherwise the file will be copied in memory!
        filterRegBean.setFilter(new ExcludePathAwareShallowETagFilter(
                "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", "/{tenant}/controller/artifacts/**",
                "/{targetid}/softwaremodules/{softwareModuleId}/artifacts/**"));
        return filterRegBean;
    }

    /**
     * Security configuration for the REST management API of the health url.
     *
     *
     */
    @Configuration
    @Order(310)
    public static class HealthSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            http.regexMatcher("/system/health").csrf().disable().httpBasic().and().sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }
    }

    /**
     * Security configuration for the REST management API.
     *
     *
     */
    @Configuration
    @Order(350)
    public static class RestSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        @Autowired
        private UserAuthenticationFilter userAuthenticationFilter;
        @Autowired
        private SystemManagement systemManagement;
        @Autowired
        private TenantAware tenantAware;
        @Autowired
        private org.springframework.boot.autoconfigure.security.SecurityProperties springSecurityProperties;

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            HttpSecurity httpSec = http.regexMatcher("\\/rest.*|\\/system.*").csrf().disable();
            if (springSecurityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            }
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
            }, RequestHeaderAuthenticationFilter.class)
                    .addFilterAfter(
                            new AuthenticationSuccessTenantMetadataCreationFilter(tenantAware, systemManagement),
                            RequestHeaderAuthenticationFilter.class)
                    .authorizeRequests().anyRequest().authenticated()
                    .antMatchers(RestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                    .hasAnyAuthority(SpPermission.SYSTEM_ADMIN).antMatchers(RestConstants.BASE_SYSTEM_MAPPING + "/**")
                    .hasAnyAuthority(SpPermission.SYSTEM_DIAG);
        }
    }

    /**
     * {@link WebSecurityConfigurer} for external (management) access.
     *
     */
    @Configuration
    @Order(400)
    @EnableVaadinSecurity
    public static class UISecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private static final String XFRAME_OPTION_DENY = "DENY";
        private static final String XFRAME_OPTION_SAMEORIGIN = "SAMEORIGIN";
        private static final String XFAME_OPTION_ALLOW_FROM = "ALLOW-FROM";
        @Autowired
        private VaadinSecurityContext vaadinSecurityContext;
        @Autowired
        private org.springframework.boot.autoconfigure.security.SecurityProperties springSecurityProperties;
        @Autowired
        private HawkbitSecurityProperties securityProperties;

        /**
         * post construct for setting the authentication success handler for the
         * vaadin security context.
         */
        @PostConstruct
        public void afterPropertiesSet() {
            this.vaadinSecurityContext.addAuthenticationSuccessHandler(redirectSaveHandler());
        }

        @Bean(name = "authenticationManager")
        @Override
        public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
        }

        /**
         * @return The VaadinRedirectStategy
         */
        @Bean
        public VaadinRedirectStrategy vaadinRedirectStrategy() {
            return new VaadinDefaultRedirectStrategy();
        }

        /**
         * @return the vaadin success authentication handler
         */
        @Bean
        public VaadinAuthenticationSuccessHandler redirectSaveHandler() {
            final VaadinUrlAuthenticationSuccessHandler handler = new TenantMetadataSavedRequestAwareVaadinAuthenticationSuccessHandler();
            handler.setRedirectStrategy(vaadinRedirectStrategy());
            handler.setDefaultTargetUrl("/UI/");
            handler.setTargetUrlParameter("r");
            return handler;
        }

        /**
         * Listener to redirect to login page after session timeout. Close the
         * vaadin session, because it's is not possible to redirect in
         * atmospehere.
         *
         * @return the servlet listener.
         */
        @Bean
        public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
            return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {

            // configuration xframe-option
            final String confXframeOption = securityProperties.getXframe().getOption();
            final String confAllowFromUri = securityProperties.getXframe().getAllowfrom();
            if (confXframeOption.equals(XFAME_OPTION_ALLOW_FROM) && confAllowFromUri.isEmpty()) {
                // if allow-from option is specified but no allowFromUri throw
                // exception
                throw new IllegalStateException("hawkbit.server.security.xframe.option has been specified as ALLOW-FROM"
                        + " but no hawkbit.server.security.xframe.allowfrom has been set, "
                        + "please ensure to set allow from URIs");
            }

            // workaround regex: we need to exclude the URL /UI/HEARTBEAT here
            // because we bound the
            // vaadin application to /UI and not to root, described in
            // vaadin-forum:
            // https://vaadin.com/forum#!/thread/3200565.
            HttpSecurity httpSec = http.regexMatcher("(?!.*HEARTBEAT)^.*\\/UI.*$")
                    // disable as CSRF is handled by Vaadin
                    .csrf().disable();

            if (springSecurityProperties.isRequireSsl()) {
                httpSec = httpSec.requiresChannel().anyRequest().requiresSecure().and();
            } else {
                LOG.info(
                        "\"******************\\n** Requires HTTPS Security has been disabled for UI, should only use for developing purposes **\\n******************\"");
            }

            // for UI integrator we allow frame integration on same origin
            httpSec.headers()
                    .addHeaderWriter(confXframeOption.equals(XFAME_OPTION_ALLOW_FROM)
                            ? new XFrameOptionsHeaderWriter(new StaticAllowFromStrategy(new URI(confAllowFromUri)))
                            : new XFrameOptionsHeaderWriter(xframeOptionFromStr(confXframeOption)))
                    .contentTypeOptions().xssProtection().httpStrictTransportSecurity().and()
                    // UI
                    .authorizeRequests().antMatchers("/UI/login/**").permitAll().antMatchers("/UI/UIDL/**").permitAll()
                    .anyRequest().authenticated().and()
                    // UI login / logout
                    .exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/UI/login/#/"))
                    .and().logout().logoutUrl("/UI/logout").logoutSuccessUrl("/UI/login/#/");
        }

        /**
         * Converts a given string into the {@link XFrameOptionsMode} enum. Only
         * {@link XFrameOptionsMode#DENY} and
         * {@link XFrameOptionsMode#SAMEORIGIN} any other string will be
         * converted to the default {@link XFrameOptionsMode#SAMEORIGIN}.
         *
         * @param xframeOption
         *            the string of the xframe option
         * @return an {@link XFrameOptionsMode} by the given string, in case
         *         string does not match an option then
         *         {@link XFrameOptionsMode#SAMEORIGIN} is returned
         */
        private static XFrameOptionsMode xframeOptionFromStr(@NotNull final String xframeOption) {
            switch (xframeOption) {
            case XFRAME_OPTION_DENY:
                return XFrameOptionsMode.DENY;
            case XFRAME_OPTION_SAMEORIGIN:
                // fall through to default because the same
            default:
                return XFrameOptionsMode.SAMEORIGIN;
            }
        }

        @Override
        public void configure(final WebSecurity webSecurity) throws Exception {
            webSecurity.ignoring().antMatchers("/documentation/**", "/VAADIN/**", "/*.*", "/v2/api-docs/**",
                    "/docs/**");
        }
    }

    /**
     * A Websecruity config to handle and filter the download ids.
     *
     *
     *
     */
    @Configuration
    @EnableWebSecurity
    @Order(200)
    public static class IdRestSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private DdiSecurityProperties securityConfiguration;

        @Autowired
        @Qualifier(CacheConstants.DOWNLOAD_ID_CACHE)
        private Cache downloadIdCache;

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            final HttpDownloadAuthenticationFilter downloadIdAuthenticationFilter = new HttpDownloadAuthenticationFilter(
                    downloadIdCache);
            downloadIdAuthenticationFilter.setAuthenticationManager(authenticationManager());

            http.csrf().disable();
            http.anonymous().disable();

            http.regexMatcher(HttpDownloadAuthenticationFilter.REQUEST_ID_REGEX_PATTERN)
                    .addFilterBefore(downloadIdAuthenticationFilter, FilterSecurityInterceptor.class);
            http.authorizeRequests().anyRequest().authenticated();
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(
                    new PreAuthTokenSourceTrustAuthenticationProvider(securityConfiguration.getRp().getTrustedIPs()));
        }

    }

}

/**
 * After a successful login on the UI we need to ensure to create the tenant
 * meta data within SP.
 *
 *
 */
class TenantMetadataSavedRequestAwareVaadinAuthenticationSuccessHandler extends VaadinUrlAuthenticationSuccessHandler {

    @Autowired
    private SystemManagement systemManagement;

    /*
     * (non-Javadoc)
     *
     * @see org.vaadin.spring.security.web.authentication.
     * SavedRequestAwareVaadinAuthenticationSuccessHandler
     * #onAuthenticationSuccess(org.springframework.security.core.
     * Authentication)
     */
    @Override
    public void onAuthenticationSuccess(final Authentication authentication) throws Exception {
        if (authentication.getClass().equals(TenantUserPasswordAuthenticationToken.class)) {
            systemManagement
                    .getTenantMetadata(((TenantUserPasswordAuthenticationToken) authentication).getTenant().toString());
        } else if (authentication.getClass().equals(UsernamePasswordAuthenticationToken.class)) {
            // TODO: vaadin4spring-ext-security does not give us the
            // fullyAuthenticatedToken
            // in the GenericVaadinSecurity class. Only the token which has been
            // created in the
            // LoginView. This needs to be changed with the update of
            // vaadin4spring 0.0.7 because it
            // has been fixed.
            systemManagement.getTenantMetadata("DEFAULT");
        }
        super.onAuthenticationSuccess(authentication);
    }
}

/**
 * Sevletfilter to create metadata after successful authentication over RESTful.
 *
 *
 */
class AuthenticationSuccessTenantMetadataCreationFilter implements Filter {

    private final TenantAware tenantAware;
    private final SystemManagement systemManagement;

    AuthenticationSuccessTenantMetadataCreationFilter(final TenantAware tenantAware,
            final SystemManagement systemManagement) {
        this.tenantAware = tenantAware;
        this.systemManagement = systemManagement;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not needed
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant != null) {
            // lazy initialize tenant meta data after successful authentication
            systemManagement.getTenantMetadata(currentTenant);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // not needed
    }
}
