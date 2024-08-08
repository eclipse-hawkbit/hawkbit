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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
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
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for OpenID Connect user management. Based on clients configuration.
 *
 * @deprecated hawkBit doesn't use/depend on clients configuration (it was back in time of integrated UI.
 */
@Configuration
@Conditional(value = ClientsConfiguredCondition.class)
@ConditionalOnProperty(prefix = "hawkbit.server.security.oAuth2OnClientsConfig", name = "enabled", havingValue = "true", matchIfMissing = true)
@Deprecated(forRemoval = true)
public class OidcUserManagementAutoConfiguration {

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

    @Bean("hawkbitOAuth2ResourceServerCustomizer")
    @ConditionalOnMissingBean
    Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer(
            final InMemoryClientRegistrationRepository clientRegistrationRepository,
            final JwtAuthoritiesExtractor authoritiesExtractor) {
        // Only get the first client registration. Testing against every client could increase the attack vector (?)
        final ClientRegistration clientRegistration =
                clientRegistrationRepository.iterator().hasNext() ? clientRegistrationRepository.iterator().next() : null;
        Assert.notNull(clientRegistration, "There must be a valid client registration");

        return configurer -> configurer.jwt(configurer2 -> {
            if (clientRegistration.getProviderDetails().getJwkSetUri() == null) {
                configurer2.decoder(JwtDecoders.fromIssuerLocation(clientRegistration.getProviderDetails().getIssuerUri()));
            } else {
                configurer2.jwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri());
            }
            configurer2.jwtAuthenticationConverter(jwt -> {
                final String defaultTenant = "DEFAULT";

                final OidcIdToken idToken = new OidcIdToken(
                        jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
                final OidcUserInfo userInfo = new OidcUserInfo(jwt.getClaims());

                final Set<GrantedAuthority> authorities = authoritiesExtractor.extract(jwt, clientRegistration);
                if (authorities.isEmpty()) {
                    throw new AccessDeniedException("No authorities found in token");
                }

                final DefaultOidcUser user = new DefaultOidcUser(authorities, idToken, userInfo);

                final OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(
                        user, authorities, clientRegistration.getRegistrationId());

                oAuth2AuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(defaultTenant, false));
                return oAuth2AuthenticationToken;
            });
        });
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
     * Utility class to extract authorities out of the jwt. It interprets the user's role as their authorities.
     */
    private record DefaultJwtAuthoritiesExtractor(GrantedAuthoritiesMapper authoritiesMapper) implements JwtAuthoritiesExtractor {

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
}