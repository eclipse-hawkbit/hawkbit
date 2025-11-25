/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for actions that are aware of the application's current tenant.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tenant {

    /**
     * Implementation might retrieve the current tenant from a session or thread-local.
     *
     * @return the current tenant
     */
    public static String currentTenant() {
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
}