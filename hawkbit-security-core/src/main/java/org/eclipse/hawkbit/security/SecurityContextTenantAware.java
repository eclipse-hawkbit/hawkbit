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

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.eclipse.hawkbit.tenancy.UserAuthoritiesResolver;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * A {@link ContextAware} (hence of {@link TenantAware}) that uses spring security context propagation
 * mechanisms and  which retrieves the ID of the tenant from the {@link SecurityContext#getAuthentication()}
 * {@link Authentication#getDetails()} which holds the {@link TenantAwareAuthenticationDetails} object.
 */
public class SecurityContextTenantAware implements ContextAware {

    public static final String SYSTEM_USER = "system";

    private static final Collection<? extends GrantedAuthority> SYSTEM_AUTHORITIES =
            List.of(new SimpleGrantedAuthority(SpringEvalExpressions.SYSTEM_ROLE));

    private final UserAuthoritiesResolver authoritiesResolver;
    private final SecurityContextSerializer securityContextSerializer;
    private final TenantResolver tenantResolver;

    /**
     * Creates the {@link SecurityContextTenantAware} based on the given {@link UserAuthoritiesResolver}.
     *
     * @param authoritiesResolver Resolver to retrieve the authorities for a given user. Must not be <code>null</code>..
     */
    public SecurityContextTenantAware(final UserAuthoritiesResolver authoritiesResolver) {
        this(authoritiesResolver, null, null);
    }

    /**
     * Creates the {@link SecurityContextTenantAware} based on the given {@link UserAuthoritiesResolver}.
     *
     * @param authoritiesResolver Resolver to retrieve the authorities for a given user. Must not be <code>null</code>.
     * @param securityContextSerializer Serializer that is used to serialize / deserialize {@link SecurityContext}s.
     */
    public SecurityContextTenantAware(
            final UserAuthoritiesResolver authoritiesResolver,
            @Nullable final SecurityContextSerializer securityContextSerializer) {
        this(authoritiesResolver, securityContextSerializer, null);
    }

    /**
     * Creates the {@link SecurityContextTenantAware} based on the given {@link UserAuthoritiesResolver}.
     *
     * @param authoritiesResolver Resolver to retrieve the authorities for a given user. Must not be <code>null</code>.
     * @param securityContextSerializer Serializer that is used to serialize / deserialize {@link SecurityContext}s.
     */
    public SecurityContextTenantAware(
            final UserAuthoritiesResolver authoritiesResolver,
            @Nullable final SecurityContextSerializer securityContextSerializer,
            @Nullable final TenantResolver tenantResolver) {
        this.authoritiesResolver = authoritiesResolver;
        this.securityContextSerializer = securityContextSerializer == null ? SecurityContextSerializer.NOP : securityContextSerializer;
        this.tenantResolver = tenantResolver == null ? new DefaultTenantResolver() : tenantResolver;
    }

    @Override
    public String getCurrentTenant() {
        return tenantResolver.resolveTenant();
    }

    @Override
    public String getCurrentUsername() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object principal = context.getAuthentication().getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                return oidcUser.getPreferredUsername();
            }
            if (principal instanceof User user) {
                return user.getUsername();
            }
        }
        return null;
    }

    @Override
    public <T> T runAsTenant(final String tenant, final TenantRunner<T> tenantRunner) {
        return runInContext(buildUserSecurityContext(tenant, SYSTEM_USER, SYSTEM_AUTHORITIES), tenantRunner::run);
    }

    @Override
    public <T> T runAsTenantAsUser(final String tenant, final String username, final TenantRunner<T> tenantRunner) {
        Objects.requireNonNull(tenant);
        Objects.requireNonNull(username);

        final List<SimpleGrantedAuthority> authorities = runAsSystem(
                () -> authoritiesResolver.getUserAuthorities(tenant, username).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        return runInContext(buildUserSecurityContext(tenant, username, authorities), tenantRunner::run);
    }

    @Override
    public Optional<String> getCurrentContext() {
        return Optional.ofNullable(SecurityContextHolder.getContext()).map(securityContextSerializer::serialize);
    }

    @Override
    public <T, R> R runInContext(final String serializedContext, final Function<T, R> function, final T t) {
        Objects.requireNonNull(serializedContext);
        Objects.requireNonNull(function);
        final SecurityContext securityContext = securityContextSerializer.deserialize(serializedContext);
        Objects.requireNonNull(securityContext);

        return runInContext(securityContext, () -> function.apply(t));
    }

    private static <T> T runInContext(final SecurityContext securityContext, final Supplier<T> supplier) {
        final SecurityContext originalContext = SecurityContextHolder.getContext();
        if (Objects.equals(securityContext, originalContext)) {
            return supplier.get();
        } else {
            SecurityContextHolder.setContext(securityContext);
            try {
                return MdcHandler.getInstance().callWithAuthRE(supplier::get);
            } finally {
                SecurityContextHolder.setContext(originalContext);
            }
        }
    }

    private static <T> T runAsSystem(final TenantRunner<T> tenantRunner) {
        final SecurityContext currentContext = SecurityContextHolder.getContext();
        SystemSecurityContext.setSystemContext(currentContext);
        try {
            return MdcHandler.getInstance().callWithAuthRE(tenantRunner::run);
        } finally {
            SecurityContextHolder.setContext(currentContext);
        }
    }

    private static SecurityContext buildUserSecurityContext(
            final String tenant, final String username, final Collection<? extends GrantedAuthority> authorities) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new AuthenticationDelegate(
                SecurityContextHolder.getContext().getAuthentication(), tenant, username, authorities));
        return securityContext;
    }

    /**
     * An {@link Authentication} implementation to delegate to an existing {@link Authentication} object except setting the details
     * specifically for a specific tenant and user.
     */
    private static final class AuthenticationDelegate implements Authentication {

        @Serial
        private static final long serialVersionUID = 1L;

        private final Authentication delegate;
        private final TenantAwareUser principal;
        private final TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails;

        private AuthenticationDelegate(final Authentication delegate, final String tenant, final String username,
                final Collection<? extends GrantedAuthority> authorities) {
            this.delegate = delegate;
            principal = new TenantAwareUser(username, username, authorities, tenant);
            tenantAwareAuthenticationDetails = new TenantAwareAuthenticationDetails(tenant, false);
        }

        @Override
        public int hashCode() {
            return delegate != null ? delegate.hashCode() : -1;
        }

        @Override
        public boolean equals(final Object another) {
            if (another instanceof Authentication anotherAuthentication) {
                return Objects.equals(delegate, anotherAuthentication) &&
                        Objects.equals(principal, anotherAuthentication.getPrincipal()) &&
                        Objects.equals(tenantAwareAuthenticationDetails, anotherAuthentication.getDetails());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return delegate != null ? delegate.toString() : null;
        }

        @Override
        public String getName() {
            return delegate != null ? delegate.getName() : null;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return delegate != null ? delegate.getAuthorities() : Collections.emptyList();
        }

        @Override
        public Object getCredentials() {
            return delegate != null ? delegate.getCredentials() : null;
        }

        @Override
        public Object getDetails() {
            return tenantAwareAuthenticationDetails;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAuthenticated() {
            return delegate == null || delegate.isAuthenticated();
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