/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Auto-configuration for OpenID Connect user management.
 *
 */
@Configuration
@Conditional(value = ClientsConfiguredCondition.class)
public class OidcUserManagementAutoConfiguration {

    /**
     * @return the oauth2 user details service to load a user from oidc user
     *         manager
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserDetailsService(JwtAuthoritiesExtractor extractor) {
        return new JwtAuthoritiesOidcUserService(extractor);
    }

    /**
     * @return the OpenID Connect authentication success handler
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationSuccessHandler oidcAuthenticationSuccessHandler() {
        return new OidcAuthenticationSuccessHandler();
    }

    /**
     * @return the OpenID Connect logout handler
     */
    @Bean
    @ConditionalOnMissingBean
    public LogoutHandler oidcLogoutHandler() {
        return new OidcLogoutHandler();
    }

    /**
     * @return a jwt authorities extractor which interprets the roles of a user as their authorities.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthoritiesExtractor jwtAuthoritiesExtractor() {
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setPrefix("");
        authorityMapper.setConvertToUpperCase(true);

        return new JwtAuthoritiesExtractor(authorityMapper);
    }

    /**
     * @return an authentication filter for using OAuth2 Bearer Tokens.
     */
    @Bean
    @ConditionalOnMissingBean
    public OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter() {
        return new OidcBearerTokenAuthenticationFilter();
    }
}

/**
 * Extended {@link OidcUserService} supporting JWT containing authorities
 */
class JwtAuthoritiesOidcUserService extends OidcUserService {

    private final JwtAuthoritiesExtractor authoritiesExtractor;

    JwtAuthoritiesOidcUserService(JwtAuthoritiesExtractor authoritiesExtractor) {
        super();

        this.authoritiesExtractor = authoritiesExtractor;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser user = super.loadUser(userRequest);
        ClientRegistration clientRegistration = userRequest.getClientRegistration();

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(authoritiesExtractor.extract(clientRegistration,
                userRequest.getAccessToken().getTokenValue()));
        if (authorities.isEmpty()) {
            return user;
        }

        String userNameAttributeName = clientRegistration.getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        OidcUser oidcUser;
        if (StringUtils.hasText(userNameAttributeName)) {
            oidcUser = new DefaultOidcUser(authorities, userRequest.getIdToken(), user.getUserInfo(),
                    userNameAttributeName);
        } else {
            oidcUser = new DefaultOidcUser(authorities, userRequest.getIdToken(), user.getUserInfo());
        }
        return oidcUser;
    }
}

/**
 * OpenID Connect Authentication Success Handler which load tenant data
 */
class OidcAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {
        if (authentication instanceof AbstractAuthenticationToken) {
            final String defaultTenant = "DEFAULT";

            AbstractAuthenticationToken token = (AbstractAuthenticationToken) authentication;
            token.setDetails(new TenantAwareAuthenticationDetails(defaultTenant, false));

            systemSecurityContext.runAsSystemAsTenant(systemManagement::getTenantMetadata, defaultTenant);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

/**
 * LogoutHandler to invalidate OpenID Connect tokens
 */
class OidcLogoutHandler extends SecurityContextLogoutHandler {

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        super.logout(request, response, authentication);

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser) {
            OidcUser user = (OidcUser) authentication.getPrincipal();
            String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue());

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForEntity(builder.toUriString(), String.class);
        }
    }
}

/**
 * Utility class to extract authorities out of the jwt. It interprets the user's role as their authorities.
 */
class JwtAuthoritiesExtractor {

    private final GrantedAuthoritiesMapper authoritiesMapper;

    private static final OAuth2Error INVALID_REQUEST = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);

    JwtAuthoritiesExtractor(GrantedAuthoritiesMapper authoritiesMapper) {
        super();

        this.authoritiesMapper = authoritiesMapper;
    }

    Collection<? extends GrantedAuthority> extract(ClientRegistration clientRegistration,
                                                                      String tokenValue) {
        Jwt token;
        try {
            // Token is already verified by spring security
            JwtDecoder jwtDecoder = new NimbusJwtDecoderJwkSupport(
                    clientRegistration.getProviderDetails().getJwkSetUri());
            token = jwtDecoder.decode(tokenValue);
        } catch (JwtException e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }

        return extract(clientRegistration.getClientId(), token.getClaims());
    }

    Collection<? extends GrantedAuthority> extract(String clientId, Map<String, Object> claims) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceMap = (Map<String, Object>) claims.get("resource_access");

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> clientResource = (Map<String, Map<String, Object>>) resourceMap.get(clientId);
        if (CollectionUtils.isEmpty(clientResource)) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) clientResource.get("roles");
        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptyList();
        }

        Collection<? extends GrantedAuthority> authorities = AuthorityUtils
                .createAuthorityList(roles.toArray(new String[0]));
        if (authoritiesMapper != null) {
            authorities = authoritiesMapper.mapAuthorities(authorities);
        }

        return authorities;
    }
}

class OidcBearerTokenAuthenticationFilter implements UserAuthenticationFilter, Filter {

    @Autowired
    private JwtAuthoritiesExtractor authoritiesExtractor;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    private ClientRegistration clientRegistration;

    void setClientRegistration(ClientRegistration clientRegistration) {
        this.clientRegistration = clientRegistration;
    }

    @Override
    public void init(final FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            final String defaultTenant = "DEFAULT";

            JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuthenticationToken.getToken();
            OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
                    jwt.getClaims());
            OidcUserInfo userInfo = new OidcUserInfo(jwt.getClaims());

            Collection<? extends GrantedAuthority> authorities = authoritiesExtractor.extract(
                    clientRegistration.getClientId(), jwt.getClaims());

            if (authorities.isEmpty()) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            DefaultOidcUser user = new DefaultOidcUser(authorities, idToken, userInfo);

            OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(
                    user, authorities, clientRegistration.getRegistrationId());

            oAuth2AuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(defaultTenant, false));

            systemSecurityContext.runAsSystemAsTenant(systemManagement::getTenantMetadata, defaultTenant);
            SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
