/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collection;
import java.util.List;

public class PreAuthHonoAuthenticationProvider extends PreAuthTokenSourceTrustAuthenticationProvider {

    public PreAuthHonoAuthenticationProvider() {
        super();
    }

    public PreAuthHonoAuthenticationProvider(final List<String> authorizedSourceIps) {
        super(authorizedSourceIps);
    }

    public PreAuthHonoAuthenticationProvider(final String... authorizedSourceIps) {
        super(authorizedSourceIps);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }

        final PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) authentication;
        final Object credentials = token.getCredentials();
        final Object principal = token.getPrincipal();
        final Object tokenDetails = token.getDetails();
        final Collection<GrantedAuthority> authorities = token.getAuthorities();

        if (!(principal instanceof HeaderAuthentication) || !(credentials instanceof Collection)) {
            throw new BadCredentialsException("The provided principal and credentials are not match");
        }

        boolean successAuthentication = false;
        for (Object object : (Collection) credentials) {
            if (object instanceof HonoCredentials) {
                if (((HonoCredentials) object).matches(((HeaderAuthentication) principal).getHeaderAuth())) {
                    successAuthentication = checkSourceIPAddressIfNeccessary(tokenDetails);;
                    break;
                }
            }
        }

        if (successAuthentication) {
            final PreAuthenticatedAuthenticationToken successToken = new PreAuthenticatedAuthenticationToken(principal,
                    credentials, authorities);
            successToken.setDetails(tokenDetails);
            return successToken;
        }

        throw new BadCredentialsException("The provided principal and credentials are not match");
    }
}
