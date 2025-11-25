/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.slf4j.MDC;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
// java:S112 - it is generic class so a generic exception is fine
@SuppressWarnings("java:S112")
public class Mdc {

    public static final String MDC_KEY_TENANT = "tenant";
    public static final String MDC_KEY_USER = "user";

    private static boolean enabled = true;

    // hook to disable (otherwise enabled by default) MDC context management
    public static void setEnabled(final boolean enabled) {
        Mdc.enabled = enabled;
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     * @throws Exception if thrown by the callable
     */
    public static <T> T withAuth(final Callable<T> callable) throws Exception {
        if (!enabled) {
            return callable.call();
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return callable.call();
        }

        final String tenant;
        if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
            tenant = tenantAwareAuthenticationDetails.tenant();
        } else {
            tenant = null;
        }

        final String user = Optional.ofNullable(Auditor.currentAuditor())
                .filter(username -> !username.equals(System.SYSTEM_USER)) // null and system are the same - system user
                .orElse(null);

        return asTenantAsUser0(tenant, user, callable);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     * Calls the {@link #withAuth(Callable)} method and wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     */
    public static <T> T withAuthRe(final Callable<T> callable) {
        try {
            return withAuth(callable);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user in the MDC context.
     *
     * @param <T> the return type
     * @param tenant the tenant to set in the MDC context
     * @param user the user to set in the MDC context
     * @param callable the callable to execute
     * @return the result
     */
    public static <T> T asTenantAsUser(final String tenant, final String user, final Callable<T> callable) throws Exception {
        if (!enabled) {
            return callable.call();
        }

        return asTenantAsUser0(tenant, user, callable);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     * Calls the {@link #asTenantAsUser(String, String, Callable)} method and wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param tenant the tenant to set in the MDC context
     * @param user the user to set in the MDC context
     * @param callable the callable to execute
     * @return the result
     */
    public static <T> T asTenantAsUserRe(final String tenant, final String user, final Callable<T> callable) {
        try {
            return asTenantAsUser(tenant, user, callable);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T asTenantAsUser0(final String tenant, final String user, final Callable<T> callable) throws Exception {
        final String currentTenant = MDC.get(MDC_KEY_TENANT);
        if (Objects.equals(currentTenant, tenant)) {
            return asUser(callable, user);
        } else {
            put(MDC_KEY_TENANT, tenant);
            try {
                return asUser(callable, user);
            } finally {
                put(MDC_KEY_TENANT, currentTenant);
            }
        }
    }

    private static <T> T asUser(final Callable<T> callable, final String user) throws Exception {
        final String currentUser = MDC.get(MDC_KEY_USER);
        if (Objects.equals(currentUser, user)) {
            return callable.call();
        } else {
            put(MDC_KEY_USER, user);
            try {
                return callable.call();
            } finally {
                put(MDC_KEY_USER, currentUser);
            }
        }
    }

    private static void put(final String key, final String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Filter {

        public static void addMdcFilter(final HttpSecurity httpSecurity) {
            httpSecurity.addFilterBefore(new OncePerRequestFilter() {

                @Override
                protected void doFilterInternal(
                        final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                        throws ServletException, IOException {
                    try {
                        Mdc.withAuth(() -> {
                            filterChain.doFilter(request, response);
                            return null;
                        });
                    } catch (final ServletException | IOException | RuntimeException e) {
                        throw e;
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }, AuthorizationFilter.class);
        }
    }
}