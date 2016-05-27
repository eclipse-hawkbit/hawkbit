/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A helper class which allows to do runtime security checks.
 *
 *
 *
 *
 */
public final class SecurityChecker {

    private SecurityChecker() {

    }

    /**
     * Checks the current {@link SecurityContext} for the permission.
     * 
     * @param permission
     *            the permission to check against the security context
     * @return {@code true} if the given permission is present in the security
     *         context otherwise {@code false}
     */
    public static boolean hasPermission(final String permission) {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            final Authentication authentication = context.getAuthentication();
            if (authentication != null) {
                for (final GrantedAuthority authority : authentication.getAuthorities()) {
                    if (authority.getAuthority().equals(permission)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
