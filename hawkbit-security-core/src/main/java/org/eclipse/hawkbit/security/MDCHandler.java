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

@NoArgsConstructor
public class MDCHandler {

    public static String MDC_KEY_TENANT = "tenant";
    public static String MDC_KEY_USER = "user";

    private static final MDCHandler SINGLETON = new MDCHandler();

    @Value("${hawkbit.logging.mdchandler.enabled:true}")
    private boolean mdcEnabled;
    @Autowired
    private SpringSecurityAuditorAware springSecurityAuditorAware;
    @Autowired
    private SystemSecurityContext securityContext;

    /**
     * @return The holder singleton instance.
     */
    public static MDCHandler getInstance() {
        return SINGLETON;
    }

    public <T> T withLogging(final Callable<T> callable) throws Exception  {
        if (!mdcEnabled) {
            return callable.call();
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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

    public void addLoggingFilter(final HttpSecurity httpSecurity) {
        httpSecurity.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
                    throws ServletException, IOException {
                try {
                    withLogging(() -> {
                        filterChain.doFilter(request, response);
                        return null;
                    });
                } catch (final RuntimeException re) {
                    throw re;
                } catch (final WrappedException we) {
                    final Throwable cause = we.getCause();
                    if (cause instanceof ServletException se) {
                        throw se;
                    } else if (cause instanceof IOException ioe) {
                        throw ioe;
                    } else {
                        throw we.toRuntimeException();
                    }
                } catch (final Exception e) {
                    // should never be here - if mdc is handler is enabled non-runtime exceptions are always wrapped
                    throw new RuntimeException(e);
                }
            }
        }, AuthorizationFilter.class);
    }

    private <T> T putUserAndCall(final Callable<T> callable) throws WrappedException {
        final String user = springSecurityAuditorAware
                .getCurrentAuditor()
                .filter(username -> !username.equals("system")) // null and system are the same - system user
                .map(username -> (securityContext.isCurrentThreadSystemCode() ? "as " : "") + username)
                .orElse(null);

        final String currentUser = MDC.get(MDC_KEY_USER);
        try {
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
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new WrappedException(e);
        }
    }

    private static void put(final String key, final String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    // Wraps catchable exceptions to rethrow
    public static class WrappedException extends Exception {

        public WrappedException(final Throwable cause) {
            super(cause);
        }

        public RuntimeException toRuntimeException() {
            return new RuntimeException(getCause() == null ? this : getCause());
        }
    }
}
