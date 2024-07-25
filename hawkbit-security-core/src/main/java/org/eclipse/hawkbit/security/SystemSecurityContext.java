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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.tenancy.TenantAware;
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

    /**
     * Autowired constructor.
     * 
     * @param tenantAware the tenant aware bean to retrieve the current tenant
     */
    public SystemSecurityContext(final TenantAware tenantAware) {
        this(tenantAware, null);
    }

    /**
     * Autowired constructor.
     *
     * @param tenantAware the tenant aware bean to retrieve the current tenant
     * @param roleHierarchy the roleHierarchy that is applied
     */
    public SystemSecurityContext(final TenantAware tenantAware, final RoleHierarchy roleHierarchy) {
        this.tenantAware = tenantAware;
        this.roleHierarchy = roleHierarchy;
    }

    /**
     * Runs a given {@link Callable} within a system security context, which is
     * permitted to call secured system code. Often the system needs to call
     * secured methods by it's own without relying on the current security
     * context e.g. if the current security context does not contain the
     * necessary permission it's necessary to execute code as system code to
     * execute necessary methods and functionality.
     * 
     * The security context will be switched to the system code and back after
     * the callable is called.
     * 
     * The system code is executed for a current tenant by using the
     * {@link TenantAware#getCurrentTenant()}.
     * 
     * @param callable
     *            the callable to call within the system security context
     * @return the return value of the {@link Callable#call()} method.
     */
    // Exception squid:S2221 - Callable declares Exception
    @SuppressWarnings("squid:S2221")
    public <T> T runAsSystem(final Callable<T> callable) {
        return runAsSystemAsTenant(callable, tenantAware.getCurrentTenant());
    }

    /**
     * Runs a given {@link Callable} within a system security context, which is
     * permitted to call secured system code. Often the system needs to call
     * secured methods by it's own without relying on the current security
     * context e.g. if the current security context does not contain the
     * necessary permission it's necessary to execute code as system code to
     * execute necessary methods and functionality.
     * 
     * The security context will be switched to the system code and back after
     * the callable is called.
     * 
     * The system code is executed for a specific given tenant by using the
     * {@link TenantAware}.
     * 
     * @param callable
     *            the callable to call within the system security context
     * @param tenant
     *            the tenant to act as system code
     * @return the return value of the {@link Callable#call()} method.
     */
    // The callable API throws a Exception and not a specific one
    @SuppressWarnings({ "squid:S2221", "squid:S00112" })
    public <T> T runAsSystemAsTenant(final Callable<T> callable, final String tenant) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            log.debug("entering system code execution");
            return tenantAware.runAsTenant(tenant, () -> {
                try {
                    setSystemContext(SecurityContextHolder.getContext());
                    return callable.call();

                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } finally {
            SecurityContextHolder.setContext(oldContext);
            log.debug("leaving system code execution");
        }
    }

    /**
     * Runs a given {@link Callable} within a system security context, which has
     * the provided {@link GrantedAuthority}s to successfully run the
     * {@link Callable}.
     * 
     * The security context will be switched to the a new
     * {@link SecurityContext} and back after the callable is called.
     * 
     * @param tenant
     *            under which the {@link Callable#call()} must be executed.
     * @param callable
     *            to call within the security context
     * @return the return value of the {@link Callable#call()} method.
     */
    // The callable API throws a Exception and not a specific one
    @SuppressWarnings({ "squid:S2221", "squid:S00112" })
    public <T> T runAsControllerAsTenant(@NotEmpty final String tenant, @NotNull final Callable<T> callable) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        List<SimpleGrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS));
        try {
            return tenantAware.runAsTenant(tenant, () -> {
                try {
                    setCustomSecurityContext(tenant, oldContext.getAuthentication().getPrincipal(), authorities);
                    return callable.call();

                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });

        } finally {
            SecurityContextHolder.setContext(oldContext);
        }
    }

    /**
     * @return {@code true} if the current running code is running as system
     *         code block.
     */
    public boolean isCurrentThreadSystemCode() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof SystemCodeAuthentication;
    }

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

    private void setCustomSecurityContext(final String tenantId, final Object principal,
            final Collection<? extends GrantedAuthority> authorities) {
        final AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), principal, authorities);
        authenticationToken.setDetails(new TenantAwareAuthenticationDetails(tenantId, true));
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(securityContextImpl);
    }

    static void setSystemContext(final SecurityContext oldContext) {
        final Authentication oldAuthentication = oldContext.getAuthentication();
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(new SystemCodeAuthentication(oldAuthentication));
        SecurityContextHolder.setContext(securityContextImpl);
    }

    /**
     * An implementation of the Spring's {@link Authentication} object which is
     * used within a system security code block and wraps the original
     * authentication object. The wrapped object contains the necessary
     * {@link SpringEvalExpressions#SYSTEM_ROLE} which is allowed to execute all
     * secured methods.
     */
    public static final class SystemCodeAuthentication implements Authentication {

        private static final long serialVersionUID = 1L;
        private static final List<SimpleGrantedAuthority> AUTHORITIES = Collections
                .singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.SYSTEM_ROLE));
        private final Authentication oldAuthentication;

        private SystemCodeAuthentication(final Authentication oldAuthentication) {
            this.oldAuthentication = oldAuthentication;
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
            return oldAuthentication != null ? oldAuthentication.getCredentials() : null;
        }

        @Override
        public Object getDetails() {
            return oldAuthentication != null ? oldAuthentication.getDetails() : null;
        }

        @Override
        public Object getPrincipal() {
            return oldAuthentication != null ? oldAuthentication.getPrincipal() : null;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(final boolean isAuthenticated) {
            // not needed
        }
    }
}
