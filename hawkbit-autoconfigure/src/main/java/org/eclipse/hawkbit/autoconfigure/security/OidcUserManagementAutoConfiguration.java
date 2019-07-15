/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Configuration
@Conditional(value = ClientsConfiguredCondition.class)
public class OidcUserManagementAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserDetailsService() {
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setPrefix("");
        authorityMapper.setConvertToUpperCase(true);

        return new JwtAuthoritiesOidcUserService(authorityMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationSuccessHandler oidcAuthenticationSuccessHandler() {
        return new OidcAuthenticationSuccessHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogoutHandler oidcLogoutHandler() {
        return new OidcLogoutHandler();
    }
}

/**
 * Extended {@link OidcUserService} supporting JWT containing authorities
 */
class JwtAuthoritiesOidcUserService extends OidcUserService {

    private final OAuth2Error INVALID_REQUEST = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);

    private final GrantedAuthoritiesMapper authoritiesMapper;

    public JwtAuthoritiesOidcUserService(GrantedAuthoritiesMapper authoritiesMapper) {
        super();

        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser user = super.loadUser(userRequest);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(extractAuthorities(userRequest));
        if (authorities.isEmpty()) {
            return user;
        }

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
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

    private Collection<? extends GrantedAuthority> extractAuthorities(OidcUserRequest userRequest) {

        Jwt token;
        try {
            // Token is already verified by spring security
            JwtDecoder jwtDecoder = new NimbusJwtDecoderJwkSupport(
                    userRequest.getClientRegistration().getProviderDetails().getJwkSetUri());
            token = jwtDecoder.decode(userRequest.getAccessToken().getTokenValue());
        } catch (JwtException e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> resourceMap = (Map<String, Object>) token.getClaims().get("resource_access");
        String clientId = userRequest.getClientRegistration().getClientId();

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
        if (authentication instanceof OAuth2AuthenticationToken) {
            final String defaultTenant = "DEFAULT";

            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
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
