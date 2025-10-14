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

import java.util.concurrent.Callable;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Interface for components that are aware of the application's current tenant.
 */
public interface TenantAware {

    /**
     * Implementation might retrieve the current tenant from a session or thread-local.
     *
     * @return the current tenant
     */
    String getCurrentTenant();

    /**
     * @return the username of the currently logged-in user
     */
    String getCurrentUsername();

    /**
     * Gives the possibility to run a certain code under a specific given {@code tenant}. Only the given {@link Callable} is executed
     * under the specific tenant e.g. under control of an {@link ThreadLocal}. After the {@link Callable} it must be ensured that the
     * original tenant before this invocation is reset.
     *
     * @param tenant the tenant which the specific code should run
     * @param callable the runner which is implemented to run this specific code under the given tenant
     * @return the return type of the {@link Callable}
     */
    <T> T runAsTenant(String tenant, Callable<T> callable);

    /**
     * Gives the possibility to run a certain code under a specific given {@code tenant} and {@code username}.
     * Only the given {@link Runnable} is executed under the specific tenant and user e.g. under control of an {@link ThreadLocal}.
     * After the {@link Runnable} it must be ensured that the original tenant before this invocation is reset.
     *
     * @param tenant the tenant which the specific code should run with
     * @param username the username which the specific code should run with
     */
    void runAsTenantAsUser(String tenant, String username, Runnable runnable);

    /**
     * Resolves the tenant from the current context.
     */
    interface TenantResolver {

        String resolveTenant();
    }

    class DefaultTenantResolver implements TenantResolver {

        @Override
        public String resolveTenant() {
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
}