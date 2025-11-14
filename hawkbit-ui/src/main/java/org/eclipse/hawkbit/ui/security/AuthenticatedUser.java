/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.security;

import java.util.Optional;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUser {

    private final AuthenticationContext authenticationContext;
    private final GrantedAuthoritiesService grantedAuthoritiesService;

    public AuthenticatedUser(final AuthenticationContext authenticationContext, GrantedAuthoritiesService grantedAuthoritiesService) {
        this.authenticationContext = authenticationContext;
        this.grantedAuthoritiesService = grantedAuthoritiesService;
    }

    public Optional<String> getName() {
        return authenticationContext.getPrincipalName();
    }

    public void logout() {
        this.getName().ifPresent(grantedAuthoritiesService::evictUserFromCache);
        authenticationContext.logout();
    }
}
