/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Service;

import com.google.common.base.Throwables;

/**
 * 
 */
@Service
public class SystemSecurityContext {

    private static final Logger logger = LoggerFactory.getLogger(SystemSecurityContext.class);

    private final TenantAware tenantAware;

    /**
     * Autowired constructor.
     * 
     * @param tenantAware
     *            the tenant aware bean to retrieve the current tenant
     */
    @Autowired
    public SystemSecurityContext(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
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
     * @param callable
     *            the callable to call within the system security context
     * @return the return value of the {@link Callable#call()} method.
     */
    // Exception squid:S2221 - Callable declares Exception
    @SuppressWarnings("squid:S2221")
    public <T> T runAsSystem(final Callable<T> callable) {
        return runAsSystemAsTenant(callable, tenantAware.getCurrentTenant());
    }

    public <T> T runAsSystemAsTenant(final Callable<T> callable, final String tenant) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            logger.debug("entering system code execution");
            return tenantAware.runAsTenant(tenant, () -> {
                try {
                    setSystemContext(SecurityContextHolder.getContext());
                    return callable.call();
                } catch (final Exception e) {
                    throw Throwables.propagate(e);
                }
            });

        } finally {
            SecurityContextHolder.setContext(oldContext);
            logger.debug("leaving system code execution");
        }
    }

    /**
     * @return {@code true} if the current running code is running as system
     *         code block.
     */
    public boolean isCurrentThreadSystemCode() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof SystemCodeAuthentication;
    }

    private static void setSystemContext(final SecurityContext oldContext) {
        final Authentication oldAuthentication = oldContext.getAuthentication();
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(new SystemCodeAuthentication(oldAuthentication));
        SecurityContextHolder.setContext(securityContextImpl);
    }

    public static class SystemCodeAuthentication implements Authentication {

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
