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

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A Service which provide to run system code.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemSecurityContext {

    // Note! There shall be no regular 'system'!
    public static final String SYSTEM_USER = "system";

    private static final Collection<? extends GrantedAuthority> SYSTEM_AUTHORITIES = List.of(new SimpleGrantedAuthority(SpRole.SYSTEM_ROLE));

    /**
     * Runs a given {@link Runnable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality. <br/>
     * The security context will be switched to the system code and back after the callable is called. <br/>
     * The system code is executed for a current tenant by using the {@link TenantAware#getCurrentTenant()}.
     *
     * @param runnable the runnable to call within the system security context
     */
    public static void runAsSystem(final Runnable runnable) {
        runAsSystemAsTenant(TenantAware.getCurrentTenant(), () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a given {@link Callable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality. <br/>
     * The security context will be switched to the system code and back after the callable is called. <br/>
     * The system code is executed for a current tenant by using the {@link TenantAware#getCurrentTenant()}.
     *
     * @param callable the callable to call within the system security context
     * @return the return value of the {@link Callable#call()} method.
     */
    public static <T> T runAsSystem(final Callable<T> callable) {
        return runAsSystemAsTenant(TenantAware.getCurrentTenant(), callable);
    }

    /**
     * Runs a given {@link Callable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality.<br/>
     * The security context will be switched to the system code and back after the callable is called.<br/>
     * The system code is executed for a specific given tenant by using the {@link TenantAware}.
     *
     * @param tenant the tenant to act as system code
     * @param callable the callable to call within the system security context
     * @return the return value of the {@link Callable#call()} method.
     */
    // The callable API throws a Exception and not a specific one
    @SuppressWarnings({ "squid:S2221", "squid:S112" })
    public static <T> T runAsSystemAsTenant(final String tenant, final Callable<T> callable) {
        final SecurityContext currentContext = SecurityContextHolder.getContext();
        try {
            log.debug("Entering system code execution");
            final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new SystemCodeAuthentication(tenant));
            return ContextAware.runInContext(securityContext, () -> {
                try {
                    return callable.call();
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            SecurityContextHolder.setContext(currentContext);
            log.debug("Leaving system code execution");
        }
    }

    /**
     * @return {@code true} if the current running code is running as system code block.
     */
    public static boolean isCurrentThreadSystemCode() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof SystemCodeAuthentication;
    }

    /**
     * An implementation of the Spring's {@link Authentication} object which is used within a system security code block and
     * wraps the original auth object. The wrapped object contains the necessary {@link SpRole#SYSTEM_ROLE}
     * which is allowed to execute all secured methods.
     */
    @SuppressWarnings("java:S4275") // java:S4275 - intentionally returns the "hold" objects
    public static final class SystemCodeAuthentication implements Authentication {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final List<SimpleGrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority(SpRole.SYSTEM_ROLE));

        private final TenantAwareAuthenticationDetails details;
        private final TenantAwareUser principal;

        private SystemCodeAuthentication(final String tenant) {
            details = new TenantAwareAuthenticationDetails(tenant, false);
            principal = new TenantAwareUser(SYSTEM_USER, SYSTEM_USER, AUTHORITIES, tenant);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return AUTHORITIES;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return details;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(final boolean isAuthenticated) {
            throw new UnsupportedOperationException();
        }
    }
}