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

    @Autowired
    private TenantAware tenantAware;

    public <T> T runAsSystem(final Callable<T> callable) {
        final SecurityContext oldContext = SecurityContextHolder.getContext();
        try {
            logger.debug("entering system code execution");
            return tenantAware.runAsTenant(tenantAware.getCurrentTenant(), () -> {
                try {
                    setSystemContext();
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

    private static void setSystemContext() {
        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setAuthentication(new SystemCodeAuthentication());
        SecurityContextHolder.setContext(securityContextImpl);
    }

    public static class SystemCodeAuthentication implements Authentication {

        private static final long serialVersionUID = 1L;
        private static final List<SimpleGrantedAuthority> AUTHORITIES = Collections
                .singletonList(new SimpleGrantedAuthority(SpringEvalExpressions.SYSTEM_ROLE));

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
            return null;
        }

        @Override
        public Object getPrincipal() {
            return null;
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
