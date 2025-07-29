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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "hawkbit.server.security.oauth2.client", name = "enabled")
public class OAuth2TokenManager {

    private final OAuth2AuthorizedClientService clientService;
    private final OAuth2AuthorizedClientManager clientManager;

    OAuth2TokenManager(
            final OAuth2AuthorizedClientService clientService,
            final OAuth2AuthorizedClientManager clientManager
    ) {
        this.clientService = clientService;
        this.clientManager = clientManager;
    }

    public String getToken(final OAuth2AuthenticationToken authentication) {
        final String currentToken = ((DefaultOidcUser) authentication.getPrincipal()).getIdToken().getTokenValue();
        String registrationId = authentication.getAuthorizedClientRegistrationId();

        // This ensures that there is a client already, otherwise we won't be able to call the manager for authorization
        OAuth2AuthorizedClient authorizedClient = clientService.loadAuthorizedClient(registrationId, authentication.getName());
        if (authorizedClient == null) return currentToken;

        // Will ensure that the token is refreshed if needed; do not rely on it being not null as it won't be available
        // during the first calls made to get the rights and generate the authorities
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId).principal(authentication).build();
        // since Spring Security 6.5 this will trigger a refresh of the id token
        authorizedClient = clientManager.authorize(request);
        if (authorizedClient == null) return currentToken;

        // we need to fetch the newly created context containing the matching token
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return ((DefaultOidcUser) securityContext.getAuthentication().getPrincipal()).getIdToken().getTokenValue();
    }
}
