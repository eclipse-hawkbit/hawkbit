/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security.uaa;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.google.common.base.Throwables;

/**
 * The Spring-Auto-Configuration implementation for integrating the UAA
 * (https://github.com/cloudfoundry/uaa) as an identity management.
 * 
 * To use the OAUTH2 redirect login flow the
 * {@link OAuth2ClientAuthenticationProcessingFilter} is listing on the path
 * {@code /uaalogin}. This will then re-direct to the configured UAA login form.
 * 
 * The {@link UserAuthenticationFilter} implementation delegates to the
 * {@link OAuth2AuthenticationProcessingFilter} which validates given bearer
 * tokens in the {@code Authorization} header '
 * {@code Authorization: bearer eyJhbGciOiJIUzI1NiIsImtpZCI6Imx}' to
 * authenticate bearer tokens for the REST API. Only the signed token is
 * verified, there is no extra round-trip back to the OAUTH2-Server (UAA).
 *
 * </p>
 * Example configuration:
 * 
 * <pre>
    uaa.client.clientId=app
    uaa.client.clientSecret=appsecret
    uaa.client.accessTokenUri=http://localhost:8080/uaa/oauth/token
    uaa.client.userAuthorizationUri=http://localhost:8080/uaa/oauth/authorize
    uaa.client.clientAuthenticationScheme=form
    uaa.resource.userInfoUri=http://localhost:8080/uaa/userinfo
    uaa.resource.jwt.keyValue=abc
 * </pre>
 * 
 */
@EnableOAuth2Client
@EnableConfigurationProperties(UaaClientProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UaaOAuthAutoConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2ClientContext oauth2ClientContext;

    @Autowired
    private UaaClientProperties uaaClientResources;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.regexMatcher("\\/uaalogin.*").addFilterBefore(ssoFilter("/uaalogin"), BasicAuthenticationFilter.class);
    }

    /**
     * @return The {@link UserPrincipalInfoTokenServices} which extract
     *         authentication, principal and authorities information from an JWT
     *         access token.
     */
    @Bean
    public UserPrincipalInfoTokenServices userPrincipalInfoTokenServices() {
        return new UserPrincipalInfoTokenServices(uaaClientResources.getResource().getUserInfoUri(),
                uaaClientResources.getClient().getClientId(), oauth2ClientContext);
    }

    /**
     * @return The {@link JwtTokenStore} verifies access tokens and extract
     *         authentication and authorities from it.
     */
    @Bean
    public JwtTokenStore jwtTokenStore() {
        final DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        accessTokenConverter.setUserTokenConverter(userPrincipalInfoTokenServices());
        final JwtAccessTokenConverter jwtTokenEnhancer = new JwtAccessTokenConverter();
        jwtTokenEnhancer.setAccessTokenConverter(accessTokenConverter);
        jwtTokenEnhancer.setSigningKey(uaaClientResources.getResource().getJwt().getKeyValue());
        jwtTokenEnhancer.setVerifierKey(uaaClientResources.getResource().getJwt().getKeyValue());
        try {
            jwtTokenEnhancer.afterPropertiesSet();
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
        return new JwtTokenStore(jwtTokenEnhancer);
    }

    /**
     * @param filter
     *            the {@link OAuth2ClientContextFilter} to register.
     * @return the Spring {@link FilterRegistrationBean} to register a filter in
     *         the spring filter-chain
     */
    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(final OAuth2ClientContextFilter filter) {
        final FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    /**
     * @return The adapter for the hawkBit {@link UserAuthenticationFilter}
     *         which delegates to the oAuth-filter mechanism to authenticate JWT
     *         bearer tokens in the hawkBit security filter chain.
     */
    @Bean
    public UserAuthenticationFilter userAuthenticationFilter() {
        return new UserAuthenticationFilterAdapter(resourceOAuthFilter());
    }

    private Filter resourceOAuthFilter() {
        final DefaultTokenServices remoteTokenService = new DefaultTokenServices();
        remoteTokenService.setTokenStore(jwtTokenStore());
        final OAuth2AuthenticationManager oauth2Manager = new OAuth2AuthenticationManager();
        oauth2Manager.setTokenServices(remoteTokenService);
        final OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
        oAuth2AuthenticationProcessingFilter.setTokenExtractor(new BearerTokenExtractor());
        oAuth2AuthenticationProcessingFilter.setAuthenticationManager(oauth2Manager);
        return oAuth2AuthenticationProcessingFilter;
    }

    private Filter ssoFilter(final String path) {
        final OAuth2ClientAuthenticationProcessingFilter oAuth2ClientAuthenticationFilter = new OAuth2ClientAuthenticationProcessingFilter(
                path);
        final SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        oAuth2ClientAuthenticationFilter.setAuthenticationSuccessHandler(successHandler);
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        successHandler.setDefaultTargetUrl("/UI");
        final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(uaaClientResources.getClient(),
                oauth2ClientContext);
        oAuth2ClientAuthenticationFilter.setRestTemplate(oAuth2RestTemplate);
        final UserPrincipalInfoTokenServices tokenServices = new UserPrincipalInfoTokenServices(
                uaaClientResources.getResource().getUserInfoUri(), uaaClientResources.getClient().getClientId(),
                oauth2ClientContext);
        tokenServices.setRestTemplate(oAuth2RestTemplate);
        tokenServices.setAuthoritiesExtractor(tokenServices);
        oAuth2ClientAuthenticationFilter.setTokenServices(tokenServices);
        return oAuth2ClientAuthenticationFilter;
    }

    private static final class UserAuthenticationFilterAdapter implements UserAuthenticationFilter {
        private final Filter delegate;

        private UserAuthenticationFilterAdapter(final Filter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void init(final FilterConfig filterConfig) throws ServletException {
            delegate.init(filterConfig);
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            delegate.doFilter(request, response, chain);
        }

        @Override
        public void destroy() {
            delegate.destroy();
        }
    }
}
