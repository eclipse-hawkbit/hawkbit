/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUser {
    private final AuthenticationContext authenticationContext;

    public AuthenticatedUser(final AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public Optional<String> getName() {
        return authenticationContext.getPrincipalName();
    }

    public void logout() {
        authenticationContext.logout();
    }
}
