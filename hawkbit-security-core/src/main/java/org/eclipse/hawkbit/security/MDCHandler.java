/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MDCHandler {

    public static String MDC_KEY_TENANT = "tenant";
    public static String MDC_KEY_USER = "user";

    private static final MDCHandler SINGLETON = new MDCHandler();

    @Value("${hawkbit.logging.mdchandler.enabled:true}")
    private boolean mdcEnabled;
    @Autowired(required = false)
    private SpringSecurityAuditorAware springSecurityAuditorAware = new SpringSecurityAuditorAware();
    @Autowired(required = false)
    private SystemSecurityContext securityContext;

    /**
     * @return The holder singleton instance.
     */
    public static MDCHandler getInstance() {
        return SINGLETON;
    }

    /**
     * Executes callable and returns the result. If MDC is enabled, it sets the tenant and / or user in the MDC context.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     * @throws Exception if thrown by the callable
     */
    public <T> T withLogging(final Callable<T> callable) throws Exception  {
        if (!mdcEnabled) {
            return callable.call();
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return callable.call();
        }

        final String tenant;
        if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
            tenant = tenantAwareAuthenticationDetails.getTenant();
        } else {
            tenant = null;
        }

        final String currentTenant = MDC.get(MDC_KEY_TENANT);
        if (Objects.equals(currentTenant, tenant)) {
            return putUserAndCall(callable);
        } else {
            put(MDC_KEY_TENANT, tenant);
            try {
                return putUserAndCall(callable);
            } finally {
                put(MDC_KEY_TENANT, currentTenant);
            }
        }
    }

    /**
     * With logging throwing Runtime Exception (wihtLoggingRE). Calls the {@link #withLogging(Callable)} method and
     * wraps any catchable exception into a {@link RuntimeException}.
     *
     * @param <T> the return type
     * @param callable the callable to execute
     * @return the result
     */
    public <T> T withLoggingRE(final Callable<T> callable)  {
        try {
            return withLogging(callable);
        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T putUserAndCall(final Callable<T> callable) throws Exception {
        final String user = springSecurityAuditorAware
                .getCurrentAuditor()
                .filter(username -> !username.equals("system")) // null and system are the same - system user
                .map(username -> (securityContext != null && securityContext.isCurrentThreadSystemCode() ? "as " : "") + username)
                .orElse(null);

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

        public static void addLoggingFilter(final HttpSecurity httpSecurity) {
            httpSecurity.addFilterBefore(new OncePerRequestFilter() {

                private final MDCHandler mdcFilter = MDCHandler.getInstance();

                @Override
                protected void doFilterInternal(
                        final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                        throws ServletException, IOException {
                    try {
                        mdcFilter.withLogging(() -> {
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