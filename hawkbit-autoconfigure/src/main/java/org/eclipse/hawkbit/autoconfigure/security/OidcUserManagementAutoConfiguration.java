/**
 * Copyright (c) 2019 Kiwigrid GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.security;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for OpenID Connect user management.
 *
 */
@Configuration
@Conditional(value = ClientsConfiguredCondition.class)
public class OidcUserManagementAutoConfiguration {

    /**
     * @return the OpenID Connect authentication success handler
     */
    @Bean
    public AuthenticationSuccessHandler oidcAuthenticationSuccessHandler(
            final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext) {
        return new OidcAuthenticationSuccessHandler(systemManagement, systemSecurityContext);
    }

    /**
     * @return a jwt authorities extractor which interprets the roles of a user
     * as their authorities.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthoritiesExtractor jwtAuthoritiesExtractor() {
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setPrefix("");
        authorityMapper.setConvertToUpperCase(true);

        return new DefaultJwtAuthoritiesExtractor(authorityMapper);
    }

    /**
     * @return the oauth2 user details service to load a user from oidc user manager
     */
    @Bean
    @ConditionalOnMissingBean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserDetailsService(
            final JwtAuthoritiesExtractor extractor) {
        return new JwtAuthoritiesOidcUserService(extractor);
    }

    /**
     * @return an authentication filter for using OAuth2 Bearer Tokens.
     */
    @Bean
    @ConditionalOnMissingBean
    OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter(
            final JwtAuthoritiesExtractor authoritiesExtractor,
            final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext) {
        return new OidcBearerTokenAuthenticationFilter(
                authoritiesExtractor, systemManagement, systemSecurityContext);
    }

    /**
     * By registering bean of such type hawkBit could be customized to extract authorities from the token.
     */
    public interface JwtAuthoritiesExtractor {

        Set<GrantedAuthority> extract(final Jwt token, final ClientRegistration clientRegistration );
    }

    /**
     * Extended {@link OidcUserService} supporting JWT containing authorities
     */
    private static class JwtAuthoritiesOidcUserService extends OidcUserService {

        private final JwtAuthoritiesExtractor authoritiesExtractor;

        JwtAuthoritiesOidcUserService(final JwtAuthoritiesExtractor authoritiesExtractor) {
            this.authoritiesExtractor = authoritiesExtractor;
        }

        @Override
        public OidcUser loadUser(final OidcUserRequest userRequest) {
            final OidcUser user = super.loadUser(userRequest);
            final ClientRegistration clientRegistration = userRequest.getClientRegistration();

            // Token is already verified by spring security
            final NimbusJwtDecoder jwtDecoder =
                    NimbusJwtDecoder
                            .withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
                            .jwsAlgorithm(SignatureAlgorithm.from(JwsAlgorithms.RS256))
                            .build();
            final Jwt token = jwtDecoder.decode(userRequest.getAccessToken().getTokenValue());
            final Set<GrantedAuthority> authorities = authoritiesExtractor.extract(token, clientRegistration);
            if (authorities.isEmpty()) {
                return user;
            }

            final String userNameAttributeName = clientRegistration.getProviderDetails().getUserInfoEndpoint()
                    .getUserNameAttributeName();
            final OidcUser oidcUser;
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
    private static class OidcAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        private final SystemManagement systemManagement;
        private final SystemSecurityContext systemSecurityContext;

        OidcAuthenticationSuccessHandler(
                final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext) {
            this.systemManagement = systemManagement;
            this.systemSecurityContext = systemSecurityContext;
        }

        @Override
        public void onAuthenticationSuccess(
                final HttpServletRequest request, final HttpServletResponse response,
                final Authentication authentication) throws ServletException, IOException {
            if (authentication instanceof AbstractAuthenticationToken token) {
                final String defaultTenant = "DEFAULT";

                token.setDetails(new TenantAwareAuthenticationDetails(defaultTenant, false));

                systemSecurityContext.runAsSystemAsTenant(systemManagement::getTenantMetadata, defaultTenant);
            }

            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    /**
     * Utility class to extract authorities out of the jwt. It interprets the user's
     * role as their authorities.
     */
    private record DefaultJwtAuthoritiesExtractor
            (GrantedAuthoritiesMapper authoritiesMapper) implements JwtAuthoritiesExtractor {

        private static final OAuth2Error INVALID_REQUEST = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);

        @Override
        public Set<GrantedAuthority> extract(final Jwt token, final ClientRegistration clientRegistration) {
            try {
                return extract(clientRegistration.getClientId(), token.getClaims());
            } catch (final JwtException e) {
                throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
            }
        }

        @SuppressWarnings("unchecked")
        private Set<GrantedAuthority> extract(final String clientId, final Map<String, Object> claims) {
            final Map<String, Object> resourceMap = (Map<String, Object>) claims.get("resource_access");
            if (CollectionUtils.isEmpty(resourceMap)) {
                return Collections.emptySet();
            }

            final Map<String, Map<String, Object>> clientResource = (Map<String, Map<String, Object>>) resourceMap
                    .get(clientId);
            if (CollectionUtils.isEmpty(clientResource)) {
                return Collections.emptySet();
            }

            final List<String> roles = (List<String>) clientResource.get("roles");
            if (CollectionUtils.isEmpty(roles)) {
                return Collections.emptySet();
            }

            final List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(roles.toArray(new String[0]));
            if (authoritiesMapper != null) {
                return new LinkedHashSet<>(authoritiesMapper.mapAuthorities(authorities));
            }

            return new LinkedHashSet<>(authorities);
        }
    }

    static class OidcBearerTokenAuthenticationFilter implements UserAuthenticationFilter, Filter {

        private final JwtAuthoritiesExtractor authoritiesExtractor;
        private final SystemManagement systemManagement;
        private final SystemSecurityContext systemSecurityContext;

        private ClientRegistration clientRegistration;

        OidcBearerTokenAuthenticationFilter(
                final JwtAuthoritiesExtractor authoritiesExtractor,
                final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext) {
            this.authoritiesExtractor = authoritiesExtractor;
            this.systemManagement = systemManagement;
            this.systemSecurityContext = systemSecurityContext;
        }

        void setClientRegistration(final ClientRegistration clientRegistration) {
            this.clientRegistration = clientRegistration;
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                final String defaultTenant = "DEFAULT";

                final Jwt jwt = jwtAuthenticationToken.getToken();
                final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
                        jwt.getClaims());
                final OidcUserInfo userInfo = new OidcUserInfo(jwt.getClaims());

                final Set<GrantedAuthority> authorities = authoritiesExtractor.extract(jwt, clientRegistration);

                if (authorities.isEmpty()) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                final DefaultOidcUser user = new DefaultOidcUser(authorities, idToken, userInfo);

                final OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(user, authorities,
                        clientRegistration.getRegistrationId());

                oAuth2AuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(defaultTenant, false));

                systemSecurityContext.runAsSystemAsTenant(systemManagement::getTenantMetadata, defaultTenant);
                SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);
            }

            chain.doFilter(request, response);
        }

        @Override
        public void init(final FilterConfig filterConfig) {
            // Nothing to do
        }

        @Override
        public void destroy() {
            // Nothing to do
        }
    }
}
