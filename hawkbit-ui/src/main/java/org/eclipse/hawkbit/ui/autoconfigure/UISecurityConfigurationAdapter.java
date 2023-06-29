/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.autoconfigure;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.MgmtUiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link WebSecurityConfigurer} for external (management) access.
 */
@Configuration
@EnableWebSecurity
@EnableVaadinSharedSecurity
@ConditionalOnClass(MgmtUiConfiguration.class)
public class UISecurityConfigurationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(UISecurityConfigurationAdapter.class);

    private static final int DOS_FILTER_ORDER = -200;

    @Autowired
    private HawkbitSecurityProperties hawkbitSecurityProperties;

    /**
     * Filter to protect the hawkBit management UI against to many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial
     * of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.ui-filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosMgmtUiFilter(final HawkbitSecurityProperties securityProperties) {
        final HawkbitSecurityProperties.Dos.Filter filterProperties = securityProperties.getDos().getUiFilter();
        final HawkbitSecurityProperties.Clients clientProperties = securityProperties.getClients();

        final FilterRegistrationBean<DosFilter> filterRegBean = new FilterRegistrationBean<>();

        filterRegBean.setFilter(new DosFilter(null, filterProperties.getMaxRead(),
                filterProperties.getMaxWrite(), filterProperties.getWhitelist(), clientProperties.getBlacklist(),
                clientProperties.getRemoteIpHeader()));

        // All URLs that can be called anonymous
        filterRegBean.setUrlPatterns(Arrays.asList("/UI/login", "/UI/login/*", "/UI/logout", "/UI/logout/*"));
        filterRegBean.setOrder(DOS_FILTER_ORDER);
        filterRegBean.setName("dosMgmtUiFilter");

        return filterRegBean;
    }

    @Bean
    AuthenticationManager authenticationManager(final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Overwriting VaadinAuthenticationSuccessHandler of default
     * VaadinSharedSecurityConfiguration
     *
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

    @Bean
    @Order(400)
    protected SecurityFilterChain filterChainUI(
            final HttpSecurity http,
            @Autowired(required = false)
            final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            @Autowired(required = false)
            final AuthenticationSuccessHandler authenticationSuccessHandler,
            final LogoutHandler logoutHandler,
            final LogoutSuccessHandler logoutSuccessHandler)
            throws Exception {
        final boolean enableOidc = oidcUserService != null && authenticationSuccessHandler != null;

        // workaround regex: we need to exclude the URL /UI/HEARTBEAT here
        // because we bound the vaadin application to /UI and not to root,
        // described in vaadin-forum:
        // https://vaadin.com/forum#!/thread/3200565.
        HttpSecurity httpSec;
        if (enableOidc) {
            httpSec = http.requestMatchers().antMatchers("/**/UI/**", "/**/oauth2/**").and();
        } else {
            httpSec = http.antMatcher("/**/UI/**");
        }
        // disable as CSRF is handled by Vaadin
        httpSec.csrf(AbstractHttpConfigurer::disable);
        // allow same origin X-Frame-Options for correct file download under
        // Safari
        httpSec.headers().frameOptions().sameOrigin();

        if (hawkbitSecurityProperties.isRequireSsl()) {
            httpSec = httpSec.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
        } else {
            LOG.info(
                    """
                    ******************
                    ** Requires HTTPS Security has been disabled for UI, should only be used for developing purposes **
                    ******************""");
        }

        if (!ObjectUtils.isEmpty(hawkbitSecurityProperties.getContentSecurityPolicy())) {
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

        return httpSec.build();
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
                        // nothing to do
                    }
                };
            }
            return super.getFirewalledRequest(request);
        }
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // No security for static content
        return (web) -> web.ignoring().antMatchers("/documentation/**", "/VAADIN/**", "/*.*", "/docs/**");
    }

    /**
     * Configuration that defines the {@link AccessDecisionManager} bean for
     * UI method security used by the Vaadin Servlet. Notice: we can not use
     * the top-level method security configuration because
     * AdviceMode.ASPECTJ is not supported.
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
}
