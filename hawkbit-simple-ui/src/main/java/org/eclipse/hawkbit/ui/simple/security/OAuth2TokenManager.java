/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(name = "hawkbitOAuth2ClientCustomizer")
public class OAuth2TokenManager {

    private final OAuth2AuthorizedClientService clientService;
    private final OAuth2AuthorizedClientManager clientManager;
    private final OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> tokenResponseClient;

    OAuth2TokenManager(
            final OAuth2AuthorizedClientService clientService,
            final OAuth2AuthorizedClientManager clientManager
    ) {
        this.clientService = clientService;
        this.clientManager = clientManager;
        this.tokenResponseClient = new RestClientRefreshTokenTokenResponseClient();
    }

    public String getToken(final OAuth2AuthenticationToken authentication) {
        return Optional.ofNullable(authorizedToken(authentication)).orElse(
                ((DefaultOidcUser) authentication.getPrincipal()).getIdToken().getTokenValue()
        );
    }

    /**
     * Tries to refresh the id token if it is expired and adds it to the request.
     */
    private String authorizedToken(final OAuth2AuthenticationToken authentication) {
        String registrationId = authentication.getAuthorizedClientRegistrationId();
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId).principal(authentication).build();

        // This ensures that there is a client already, otherwise we won't be able to call the manager for authorization
        OAuth2AuthorizedClient authorizedClient = clientService.loadAuthorizedClient(registrationId, authentication.getName());
        if (authorizedClient == null) return null;

        // Will ensure that the token is refreshed if needed; do not rely on it being not null as it won't be available
        // during the first calls made to get the rights and generate the authorities
        OAuth2AuthorizedClient refreshClient = clientManager.authorize(request);
        if (refreshClient == null) return null;

        // A small trick to refresh the token if it is expired; the current spring version does not refresh the ID Token when the Access Token is refreshed
        // This won't be necessary after Spring Security 6.5; cf. https://github.com/spring-projects/spring-security/pull/16589
        OAuth2AccessToken accessToken = refreshClient.getAccessToken();
        OAuth2RefreshToken refreshToken = refreshClient.getRefreshToken();
        ClientRegistration clientRegistration = refreshClient.getClientRegistration();
        // if this is null, please request it via the scopes
        if (refreshToken == null) return null;

        OAuth2RefreshTokenGrantRequest refreshTokenGrantRequest = new OAuth2RefreshTokenGrantRequest(
                clientRegistration, accessToken, refreshToken);
        OAuth2AccessTokenResponse response = tokenResponseClient.getTokenResponse(refreshTokenGrantRequest);
        return (String) response.getAdditionalParameters().get("id_token");
    }
}
