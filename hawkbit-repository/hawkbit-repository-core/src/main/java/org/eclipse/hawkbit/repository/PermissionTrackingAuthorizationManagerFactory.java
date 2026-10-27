/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.auth.PermissionTracker;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.authorization.AuthorizationResult;

/**
 * Decorates an {@link AuthorizationManagerFactory} in order to {@link PermissionTracker#record(String) record} the authorities
 * granted by {@code hasAuthority} / {@code hasAnyAuthority} expressions. Since Spring Security 7 the
 * {@link org.springframework.security.access.expression.SecurityExpressionRoot} authority checks (which are final) delegate to the
 * factory, so this is the hook to get the checked authority names.
 */
class PermissionTrackingAuthorizationManagerFactory<T> implements AuthorizationManagerFactory<T> {

    private final AuthorizationManagerFactory<T> delegate;

    PermissionTrackingAuthorizationManagerFactory(final AuthorizationManagerFactory<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuthorizationManager<T> hasAuthority(final String authority) {
        final AuthorizationManager<T> authorizationManager = delegate.hasAuthority(authority);
        return (authentication, object) -> {
            final AuthorizationResult result = authorizationManager.authorize(authentication, object);
            if (result != null && result.isGranted()) {
                PermissionTracker.record(authority);
            }
            return result;
        };
    }

    @Override
    public AuthorizationManager<T> hasAnyAuthority(final String... authorities) {
        // evaluated one by one (as an "or") in order to record the authority that actually grants access
        return (authentication, object) -> {
            for (final String authority : authorities) {
                final AuthorizationResult result = hasAuthority(authority).authorize(authentication, object);
                if (result != null && result.isGranted()) {
                    return result;
                }
            }
            return new AuthorizationDecision(false);
        };
    }

    @Override
    public AuthorizationManager<T> permitAll() {
        return delegate.permitAll();
    }

    @Override
    public AuthorizationManager<T> denyAll() {
        return delegate.denyAll();
    }

    @Override
    public AuthorizationManager<T> hasRole(final String role) {
        return delegate.hasRole(role);
    }

    @Override
    public AuthorizationManager<T> hasAnyRole(final String... roles) {
        return delegate.hasAnyRole(roles);
    }

    @Override
    public AuthorizationManager<T> hasAllRoles(final String... roles) {
        return delegate.hasAllRoles(roles);
    }

    @Override
    public AuthorizationManager<T> hasAllAuthorities(final String... authorities) {
        return delegate.hasAllAuthorities(authorities);
    }

    @Override
    public AuthorizationManager<T> authenticated() {
        return delegate.authenticated();
    }

    @Override
    public AuthorizationManager<T> fullyAuthenticated() {
        return delegate.fullyAuthenticated();
    }

    @Override
    public AuthorizationManager<T> rememberMe() {
        return delegate.rememberMe();
    }

    @Override
    public AuthorizationManager<T> anonymous() {
        return delegate.anonymous();
    }
}
