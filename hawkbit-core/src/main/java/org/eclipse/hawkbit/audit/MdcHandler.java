/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.audit;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
// java:S6548 - singleton holder ensures static access to spring resources in some places
// java:S112 - it is generic class so a generic exception is fine
@SuppressWarnings({ "java:S6548", "java:S112" })
public class MdcHandler {

    public static final String MDC_KEY_TENANT = "tenant";
    public static final String MDC_KEY_USER = "user";

    private static final MdcHandler SINGLETON = new MdcHandler();

    @Value("${hawkbit.logging.mdchandler.enabled:true}")
    private boolean mdcEnabled;

    private HawkbitAuditorAware springSecurityAuditorAware = new HawkbitAuditorAware();

    private HawkbitAuditorAware getSpringSecurityAuditorAware() {
        return springSecurityAuditorAware;
    }

    /**
     * @return The holder singleton instance.
     */
    public static MdcHandler getInstance() {
        return SINGLETON;
    }

    @Autowired(required = false) // spring setter injection
    public void setSpringSecurityAuditorAware(final HawkbitAuditorAware springSecurityAuditorAware) {
        this.springSecurityAuditorAware = springSecurityAuditorAware;
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     * @throws Exception if thrown by the callable
     */
    public <T> T callWithAuth(final Callable<T> callable) throws Exception {
        if (!mdcEnabled) {
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

        final String user = springSecurityAuditorAware
                .getCurrentAuditor()
                .filter(username -> !username.equals(SystemSecurityContext.SYSTEM_USER)) // null and system are the same - system user
                .orElse(null);

        return callWithTenantAndUser0(callable, tenant, user);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     * Calls the {@link #callWithAuth(Callable)} method and wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     */
    public <T> T callWithAuthRE(final Callable<T> callable) {
        try {
            return callWithAuth(callable);
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
     * @param callable the callable to execute
     * @param tenant the tenant to set in the MDC context
     * @param user the user to set in the MDC context
     * @return the result
     */
    public <T> T callWithTenantAndUser(final Callable<T> callable, final String tenant, final String user) throws Exception {
        if (!mdcEnabled) {
            return callable.call();
        }

        return callWithTenantAndUser0(callable, tenant, user);
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user from the auth in the MDC context.
     * Calls the {@link #callWithTenantAndUser(Callable, String, String)} method and wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @param tenant the tenant to set in the MDC context
     * @param user the user to set in the MDC context
     * @return the result
     */
    public <T> T callWithTenantAndUserRE(final Callable<T> callable, final String tenant, final String user) {
        try {
            return callWithTenantAndUser(callable, tenant, user);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T callWithTenantAndUser0(final Callable<T> callable, final String tenant, final String user) throws Exception {
        final String currentTenant = MDC.get(MDC_KEY_TENANT);
        if (Objects.equals(currentTenant, tenant)) {
            return callWithUser(callable, user);
        } else {
            put(MDC_KEY_TENANT, tenant);
            try {
                return callWithUser(callable, user);
            } finally {
                put(MDC_KEY_TENANT, currentTenant);
            }
        }
    }

    private static <T> T callWithUser(final Callable<T> callable, final String user) throws Exception {
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

                private final MdcHandler mdcFilter = MdcHandler.getInstance();

                @Override
                protected void doFilterInternal(
                        final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                        throws ServletException, IOException {
                    try {
                        mdcFilter.callWithAuth(() -> {
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