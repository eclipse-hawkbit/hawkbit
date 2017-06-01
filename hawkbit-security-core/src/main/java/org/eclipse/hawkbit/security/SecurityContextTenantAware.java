/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * A {@link TenantAware} implemenation which retrieves the ID of the tenant from
 * the {@link SecurityContext#getAuthentication()}
 * {@link Authentication#getDetails()} which holds the
 * {@link TenantAwareAuthenticationDetails} object.
 *
 */
public class SecurityContextTenantAware implements TenantAware {

    @Override
    public String getCurrentTenant() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object principal = context.getAuthentication().getPrincipal();
            if (context.getAuthentication().getDetails() instanceof TenantAwareAuthenticationDetails) {
                return ((TenantAwareAuthenticationDetails) context.getAuthentication().getDetails()).getTenant();
            } else if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getTenant();
            }
        }
        return null;
    }

    @Override
    public <T> T runAsTenant(final String tenant, final TenantRunner<T> callable) {
        final SecurityContext originalContext = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(buildSecurityContext(tenant));
            return callable.run();
        } finally {
            SecurityContextHolder.setContext(originalContext);
        }
    }

    private static SecurityContext buildSecurityContext(final String tenant) {
        final SecurityContextImpl securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new AuthenticationDelegate(SecurityContextHolder.getContext().getAuthentication(), tenant));
        return securityContext;
    }

    /**
     * An {@link Authentication} implementation to delegate to an existing
     * {@link Authentication} object except setting the details specifically for
     * a specific tenant.
     */
    private static final class AuthenticationDelegate implements Authentication {
        private static final long serialVersionUID = 1L;

        private static final String SYSTEM_USER = "system";
        private static final Collection<? extends GrantedAuthority> SYSTEM_AUTHORITIES = Arrays
                .asList(new SimpleGrantedAuthority(SpringEvalExpressions.SYSTEM_ROLE));
        private final Authentication delegate;

        private final UserPrincipal systemPrincipal;

        private final TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails;

        private AuthenticationDelegate(final Authentication delegate, final String tenant) {
            this.delegate = delegate;
            this.systemPrincipal = new UserPrincipal(SYSTEM_USER, SYSTEM_USER, SYSTEM_USER, SYSTEM_USER, SYSTEM_USER,
                    null, tenant, SYSTEM_AUTHORITIES);
            tenantAwareAuthenticationDetails = new TenantAwareAuthenticationDetails(tenant, false);
        }

        @Override
        public boolean equals(final Object another) {
            if (delegate != null) {
                return delegate.equals(another);
            } else if (another == null) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return (delegate != null) ? delegate.toString() : null;
        }

        @Override
        public int hashCode() {
            return (delegate != null) ? delegate.hashCode() : -1;
        }

        @Override
        public String getName() {
            return (delegate != null) ? delegate.getName() : null;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return (delegate != null) ? delegate.getAuthorities() : Collections.emptyList();
        }

        @Override
        public Object getCredentials() {
            return (delegate != null) ? delegate.getCredentials() : null;
        }

        @Override
        public Object getDetails() {
            return tenantAwareAuthenticationDetails;
        }

        @Override
        public Object getPrincipal() {
            return systemPrincipal;
        }

        @Override
        public boolean isAuthenticated() {
            return (delegate != null) ? delegate.isAuthenticated() : true;
        }

        @Override
        public void setAuthenticated(final boolean isAuthenticated) {
            if (delegate == null) {
                return;
            }
            delegate.setAuthenticated(isAuthenticated);
        }
    }
}
