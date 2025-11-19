/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Utility class for actions that are aware of the application's current tenant.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TenantAware {

    /**
     * Implementation might retrieve the current tenant from a session or thread-local.
     *
     * @return the current tenant
     */
    public static String getCurrentTenant() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object principal = context.getAuthentication().getPrincipal();
            if (context.getAuthentication().getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
                return tenantAwareAuthenticationDetails.tenant();
            } else if (principal instanceof TenantAwareUser tenantAwareUser) {
                return tenantAwareUser.getTenant();
            }
        }
        return null;
    }

    /**
     * @return the username of the currently logged-in user
     */
    public static String getCurrentUsername() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object principal = context.getAuthentication().getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                return oidcUser.getPreferredUsername();
            }
            if (principal instanceof User user) {
                return user.getUsername();
            }
        }
        return null;
    }
}