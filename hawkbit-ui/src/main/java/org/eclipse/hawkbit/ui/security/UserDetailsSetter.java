/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Component
@Slf4j
class UserDetailsSetter extends OncePerRequestFilter {

    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final GrantedAuthoritiesService grantedAuthoritiesService;

    @SuppressWarnings("java:S1066") // java:S1066 - readability preferred
    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();
        final Authentication newAuthentication;

        if (!(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            final Collection<? extends GrantedAuthority> grantedAuthorities = grantedAuthoritiesService.getGrantedAuthorities(authentication);
            if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
                newAuthentication = new OAuth2AuthenticationToken(
                        oAuth2AuthenticationToken.getPrincipal(), grantedAuthorities,
                        oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
                if (authentication.getPrincipal() instanceof OidcUser user) {
                    // if there is no refresh token and the access token is expired then re-login is required
                    if (user.getIdToken().getExpiresAt() != null && Instant.now().isAfter(user.getIdToken().getExpiresAt())) {
                        throw new AccountExpiredException("Token expired");
                    }
                }
            } else {
                newAuthentication = new UsernamePasswordAuthenticationToken(
                        authentication.getName(), authentication.getCredentials(), grantedAuthorities);
            }

            securityContextHolderStrategy.getContext().setAuthentication(newAuthentication);
        }

        // proceed with the filter chain
        filterChain.doFilter(request, response);
    }
}