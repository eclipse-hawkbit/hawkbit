/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.ObjectUtils;

/**
 * A Service which provide to run system code.
 */
@Slf4j
public class SystemSecurityContext {

    private final TenantAware tenantAware;
    private final RoleHierarchy roleHierarchy;

    public SystemSecurityContext(final TenantAware tenantAware) {
        this(tenantAware, null);
    }

    public SystemSecurityContext(final TenantAware tenantAware, final RoleHierarchy roleHierarchy) {
        this.tenantAware = tenantAware;
        this.roleHierarchy = roleHierarchy;
    }

    /**
     * Runs a given {@link Runnable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality. <br/>
     * The security context will be switched to the system code and back after the callable is called. <br/>
     * The system code is executed for a current tenant by using the {@link TenantAware#getCurrentTenant()}.
     *
     * @param runnable the runnable to call within the system security context
     */
    public void runAsSystem(final Runnable runnable) {
        runAsSystemAsTenant(() -> {
            runnable.run();
            return null;
        }, tenantAware.getCurrentTenant());
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
    public <T> T runAsSystem(final Callable<T> callable) {
        return runAsSystemAsTenant(callable, tenantAware.getCurrentTenant());
    }

    /**
     * Runs a given {@link Callable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality.<br/>
     * The security context will be switched to the system code and back after the callable is called.<br/>
     * The system code is executed for a specific given tenant by using the {@link TenantAware}.
     *
     * @param callable the callable to call within the system security context
     * @param tenant the tenant to act as system code
     * @return the return value of the {@link Callable#call()} method.
     */
    // The callable API throws a Exception and not a specific one
    @SuppressWarnings({ "squid:S2221", "squid:S00112" })
    public <T> T runAsSystemAsTenant(final Callable<T> callable, final String tenant) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            log.debug("Entering system code execution");
            return tenantAware.runAsTenant(tenant, () -> {
                setSystemContext(SecurityContextHolder.getContext());
                return MdcHandler.getInstance().callWithAuthRE(callable);
            });
        } finally {
            SecurityContextHolder.setContext(oldContext);
            log.debug("Leaving system code execution");
        }
    }

    /**
     * Runs a given {@link Callable} within a system security context, which has the provided {@link GrantedAuthority}s to successfully
     * run the {@link Callable}.<br/>
     * The security context will be switched to a new {@link SecurityContext} and back after the callable is called.
     *
     * @param tenant under which the {@link Callable#call()} must be executed.
     * @param callable to call within the security context
     * @return the return value of the {@link Callable#call()} method.
     */
    public <T> T runAsControllerAsTenant(@NotEmpty final String tenant, @NotNull final Callable<T> callable) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(SpRole.CONTROLLER_ROLE_ANONYMOUS));
        try {
            return tenantAware.runAsTenant(tenant, () -> {
                setCustomSecurityContext(tenant, oldContext.getAuthentication().getPrincipal(), authorities);
                return MdcHandler.getInstance().callWithAuthRE(callable);
            });
        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    /**
     * @return {@code true} if the current running code is running as system code block.
     */
    public static boolean isCurrentThreadSystemCode() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof SystemCodeAuthentication;
    }

    @SuppressWarnings("java:S3776") // java:S3776 - better in one place for better readability
    public boolean hasPermission(final String permission) {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            final Authentication authentication = context.getAuthentication();
            if (authentication != null) {
                Collection<? extends GrantedAuthority> grantedAuthorities = authentication.getAuthorities();
                if (!ObjectUtils.isEmpty(grantedAuthorities)) {
                    if (roleHierarchy != null) {
                        grantedAuthorities = roleHierarchy.getReachableGrantedAuthorities(grantedAuthorities);
                    }
                    for (final GrantedAuthority authority : grantedAuthorities) {
                        if (authority.getAuthority().equals(permission)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    static void setSystemContext(final SecurityContext oldContext) {
        final Authentication oldAuthentication = oldContext.getAuthentication();
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(new SystemCodeAuthentication(oldAuthentication));
        SecurityContextHolder.setContext(securityContextImpl);
    }

    private void setCustomSecurityContext(
            final String tenantId, final Object principal, final Collection<? extends GrantedAuthority> authorities) {
        final AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), principal, authorities);
        authenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenantId, true));
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(securityContextImpl);
    }

    /**
     * An implementation of the Spring's {@link Authentication} object which is used within a system security code block and
     * wraps the original authentication object. The wrapped object contains the necessary {@link SpRole#SYSTEM_ROLE}
     * which is allowed to execute all secured methods.
     */
    @SuppressWarnings("java:S4275") // java:S4275 - intentionally returns the "hold" objects
    public static final class SystemCodeAuthentication implements Authentication {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final List<SimpleGrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority(SpRole.SYSTEM_ROLE));

        private final Holder credentials;
        private final Holder details;
        private final Holder principal;

        private SystemCodeAuthentication(final Authentication oldAuthentication) {
            credentials = new Holder(oldAuthentication != null ? oldAuthentication.getCredentials() : null);
            details = new Holder(oldAuthentication != null ? oldAuthentication.getDetails() : null);
            principal = new Holder(oldAuthentication != null ? oldAuthentication.getPrincipal() : null);
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
            return credentials.obj;
        }

        @Override
        public Object getDetails() {
            return details.obj;
        }

        @Override
        public Object getPrincipal() {
            return principal.obj;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(final boolean isAuthenticated) {
            throw new UnsupportedOperationException();
        }

        // Serializable wrapper that ensures that the content will be serialized only if it is Serializable
        private static class Holder implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            private Object obj;

            private Holder(final Object obj) {
                this.obj = obj;
            }

            @Serial
            private void writeObject(final ObjectOutputStream oos) throws IOException {
                oos.writeObject(obj instanceof Serializable ? obj : null);
            }

            @Serial
            private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
                obj = ois.readObject();
            }
        }
    }
}