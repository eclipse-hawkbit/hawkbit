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
    // in the MDC the default actor key is "user"
    public static final String MDC_KEY_ACTOR = System.getProperty(
            "hawkbit.mdc.actor.key", // first by priority: system property
            Optional.ofNullable(System.getenv("HAWKBIT_MDC_ACTOR_KEY")) // second by priority: environment variable
                    .orElse("user")); // default if not set

    private static boolean enabled = true;

    // hook to disable (otherwise enabled by default) MDC context management
    public static void setEnabled(final boolean enabled) {
        Mdc.enabled = enabled;
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or actor from the authentication in the MDC context.
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

        final String actor = Optional.ofNullable(AccessContext.actor())
                .filter(ctxActor -> !ctxActor.equals(AccessContext.SYSTEM_ACTOR)) // null and system are the same - system actor
                .orElse(null);

        return asTenantAsActor0(tenant, actor, callable);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or actor from the authentication in the MDC context.
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
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or actor in the MDC context.
     *
     * @param <T> the return type
     * @param tenant the tenant to set in the MDC context
     * @param actor the actor to set in the MDC context
     * @param callable the callable to execute
     * @return the result
     */
    public static <T> T asTenantAsActor(final String tenant, final String actor, final Callable<T> callable) throws Exception {
        if (!enabled) {
            return callable.call();
        }

        return asTenantAsActor0(tenant, actor, callable);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or actor from the authentication in the MDC context.
     * Calls the {@link #asTenantAsActor(String, String, Callable)} method and wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param tenant the tenant to set in the MDC context
     * @param actor the actor to set in the MDC context
     * @param callable the callable to execute
     * @return the result
     */
    public static <T> T asTenantAsActorRe(final String tenant, final String actor, final Callable<T> callable) {
        try {
            return asTenantAsActor(tenant, actor, callable);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T asTenantAsActor0(final String tenant, final String actor, final Callable<T> callable) throws Exception {
        final String currentTenant = MDC.get(MDC_KEY_TENANT);
        if (Objects.equals(currentTenant, tenant)) {
            return asActor(callable, actor);
        } else {
            put(MDC_KEY_TENANT, tenant);
            try {
                return asActor(callable, actor);
            } finally {
                put(MDC_KEY_TENANT, currentTenant);
            }
        }
    }

    private static <T> T asActor(final Callable<T> callable, final String actor) throws Exception {
        final String currentActor = MDC.get(MDC_KEY_ACTOR);
        if (Objects.equals(currentActor, actor)) {
            return callable.call();
        } else {
            put(MDC_KEY_ACTOR, actor);
            try {
                return callable.call();
            } finally {
                put(MDC_KEY_ACTOR, currentActor);
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