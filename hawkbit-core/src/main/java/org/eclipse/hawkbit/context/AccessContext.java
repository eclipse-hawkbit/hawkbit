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
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * A 'static' class providing methods related to access context:
 * <ul>
 *     <li>read / lookup - find out the current tenant, principal (actor), security context</li>
 *     <li>switch context - run code as system, as system but scoped for a tenant, with a specific context</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class AccessContext {

    // Note! There shall be no regular 'system'!
    public static final String SYSTEM_ACTOR = "system";

    /**
     * Return the current context encoded as a {@link String}. Depending on the implementation it could,
     * for instance, be a serialized context or a reference to such.
     *
     * @return could be empty if there is nothing to serialize or context aware is not supported.
     */
    public static Optional<String> securityContext() {
        return Optional.ofNullable(SecurityContextHolder.getContext()).map(AccessContext::serialize);
    }

    /**
     * Implementation might retrieve the current tenant from a session or thread-local.
     *
     * @return the current tenant
     */
    public static String tenant() {
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            final Object principal = context.getAuthentication().getPrincipal();
            if (context.getAuthentication().getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails) {
                return tenantAwareAuthenticationDetails.tenant();
            } else if (principal instanceof TenantAwareUser tenantAwareUser) {
                return tenantAwareUser.getTenant();
            }
        }
        return null;
    }

    // Sometimes 'system' need to override the auditor when do create/modify actions in context of an actor
    private static final ThreadLocal<String> ACTOR_OVERRIDE = new ThreadLocal<>();

    // Return the current actor / auditor / principal name. It could be a user (person), technical user, device, etc.
    public static String actor() {
        if (ACTOR_OVERRIDE.get() != null) {
            return ACTOR_OVERRIDE.get();
        } else {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (isAuthenticationInvalid(authentication)) {
                return null;
            } else {
                return resolve(authentication);
            }
        }
    }

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param serializedContext created by {@link #securityContext()}. Must be non-<code>null</code>.
     * @param runnable runnable to run in the reconstructed context. Must be non-<code>null</code>.
     */
    public static void withSecurityContext(final String serializedContext, final Runnable runnable) {
        Objects.requireNonNull(runnable);
        withSecurityContext(serializedContext, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Wrap a specific execution / call in a known and pre-serialized context.
     *
     * @param <T> the type of the output of the supplier
     * @param serializedContext created by {@link #securityContext()}. Must be non-<code>null</code>.
     * @param supplier function to call in the reconstructed context. Must be non-<code>null</code>.
     * @return the function result
     */
    public static <T> T withSecurityContext(final String serializedContext, final Supplier<T> supplier) {
        Objects.requireNonNull(serializedContext);
        Objects.requireNonNull(supplier);
        final SecurityContext securityContext = deserialize(serializedContext);
        Objects.requireNonNull(securityContext);

        return withSecurityContext(securityContext, supplier);
    }

    public static <T> T withSecurityContext(final SecurityContext securityContext, final Supplier<T> supplier) {
        final SecurityContext originalContext = SecurityContextHolder.getContext();
        if (Objects.equals(securityContext, originalContext)) {
            return supplier.get();
        } else {
            SecurityContextHolder.setContext(securityContext);
            try {
                return Mdc.withAuthRe(supplier::get);
            } finally {
                SecurityContextHolder.setContext(originalContext);
            }
        }
    }

    /**
     * Runs a given {@link Runnable} within a current authorities with set tenant in the context.
     *
     * @param tenant the tenant to be set in context.
     * @param runnable the runnable to call within the tenant context
     */
    public static void asTenant(final String tenant, final Runnable runnable) {
        asTenant(tenant, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a given {@link Supplier} within a current authorities with set tenant in the context.
     *
     * @param tenant the tenant to be set in context.
     * @param supplier the supplier to call within the tenant context
     * @return the return value of the {@link Supplier#get()} method.
     */
    public static <T> T asTenant(final String tenant, final Supplier<T> supplier) {
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new AuthenticationDelegate(
                tenant, Optional.ofNullable(actor()).orElse(SYSTEM_ACTOR),
                SecurityContextHolder.getContext().getAuthentication()));
        return withSecurityContext(securityContext, supplier);
    }

    public static void asActor(final String actor, final Runnable runnable) {
        asActor(actor, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T asActor(final String actor, final Supplier<T> supplier) {
        final String currentAuditor = ACTOR_OVERRIDE.get();
        try {
            setActor(actor);
            return supplier.get();
        } finally {
            setActor(currentAuditor);
        }
    }

    /**
     * Runs a given {@link Runnable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality. <br/>
     * The security context will be switched to the system code and back after the supplier is called. <br/>
     * The system code is executed for a current tenant by using the {@link AccessContext#tenant()}.
     *
     * @param runnable the runnable to run within the system security context
     */
    public static void asSystem(final Runnable runnable) {
        asSystemAsTenant(tenant(), () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a given {@link Supplier} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality. <br/>
     * The security context will be switched to the system code and back after the supplier is called. <br/>
     * The system code is executed for a current tenant by using the {@link AccessContext#tenant()}.
     *
     * @param supplier the supplier to call within the system security context
     * @return the return value of the {@link Supplier#get()} method.
     */
    public static <T> T asSystem(final Supplier<T> supplier) {
        return asSystemAsTenant(tenant(), supplier);
    }

    /**
     * Runs a given {@link Runnable} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality.<br/>
     * The security context will be switched to the system code and back after the runnable is run.<br/>
     * The system code is executed for a specific given tenant by using the {@link AccessContext}.
     *
     * @param tenant the tenant to act as system code
     * @param runnable the runnable to run within the system security context
     */
    public static void asSystemAsTenant(final String tenant, final Runnable runnable) {
        asSystemAsTenant(tenant, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Runs a given {@link Supplier} within a system security context, which is permitted to call secured system code. Often the system needs
     * to call secured methods by its own without relying on the current security context e.g. if the current security context does not contain
     * the necessary permission it's necessary to execute code as system code to execute necessary methods and functionality.<br/>
     * The security context will be switched to the system code and back after the supplier is run.<br/>
     * The system code is executed for a specific given tenant by using the {@link AccessContext}.
     *
     * @param tenant the tenant to act as system code
     * @param supplier the supplier to call within the system security context
     * @return the return value of the {@link Supplier#get()} method.
     */
    public static <T> T asSystemAsTenant(final String tenant, final Supplier<T> supplier) {
        log.debug("Entering system code execution");
        try {
            final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new SystemCodeAuthentication(tenant));
            return withSecurityContext(securityContext, supplier);
        } finally {
            log.debug("Leaving system code execution");
        }
    }

    private static void setActor(final String currentAuditor) {
        if (currentAuditor == null) {
            ACTOR_OVERRIDE.remove();
        } else {
            ACTOR_OVERRIDE.set(currentAuditor);
        }
    }

    /**
     * @return {@code true} if the current running code is running as system code block.
     */
    public static boolean isCurrentThreadSystemCode() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof SystemCodeAuthentication;
    }

    private static String resolve(final Authentication authentication) {
        if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareDetails && tenantAwareDetails.controller()) {
            return "CONTROLLER_PLUG_AND_PLAY";
        }
        final Object principal = authentication.getPrincipal();
        if (principal instanceof ActorAware actorAware) {
            return actorAware.getActor();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }
        return principal.toString();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Return security context as string (could be just a reference)
     *
     * @param securityContext the security context
     * @return the securityContext as string
     */
    @SuppressWarnings("java:S112") // java:S112 - generic method
    private static String serialize(final SecurityContext securityContext) {
        Objects.requireNonNull(securityContext);
        try {
            return OBJECT_MAPPER.writeValueAsString(new SecCtxInfo(securityContext));
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserialize security context
     *
     * @param securityContextString string representing the security context
     * @return deserialized security context
     */
    @SuppressWarnings("java:S112") // java:S112 - generic method
    private static SecurityContext deserialize(final String securityContextString) {
        Objects.requireNonNull(securityContextString);
        final String securityContextTrimmed = securityContextString.trim();
        try {
            return OBJECT_MAPPER.readerFor(SecCtxInfo.class).<SecCtxInfo> readValue(securityContextTrimmed).toSecurityContext();
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isAuthenticationInvalid(final Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null;
    }

    public interface ActorAware {

        String getActor();
    }

    // simplified info for the security context keeping just the basic info needed for background execution of
    // controller auth is not supported - always is false
    // only authenticated user is supported
    @NoArgsConstructor
    @Data
    private static class SecCtxInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String tenant;
        // auditor / username (auth principal name)
        private String auditor = "n/a"; // default value "n/a" is used only on deserialization if field is missing
        @JsonProperty(required = true)
        private String[] authorities;

        private SecCtxInfo(final SecurityContext securityContext) {
            final Authentication authentication = securityContext.getAuthentication();
            if (!authentication.isAuthenticated()) {
                throw new IllegalStateException("Only authenticated context could be serialized");
            }
            if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareDetails) {
                if (tenantAwareDetails.controller()) {
                    throw new IllegalStateException("Controller auth context is not supported");
                }
                tenant = tenantAwareDetails.tenant();
            } else if (authentication.getPrincipal() instanceof TenantAwareUser tenantAwareUser) {
                tenant = tenantAwareUser.getTenant();
            }

            // keep the auditor, ofr audit purposes,
            // sets principal to the resolved auditor and then deserialized auth will return it as principal
            // since the class is not known to auditor aware - it shall used default - principal as auditor
            auditor = resolve(authentication);
            authorities = authentication.getAuthorities().stream().map(Object::toString).toArray(String[]::new);
        }

        private SecurityContext toSecurityContext() {
            final SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            final Object details = tenant == null ? null : new TenantAwareAuthenticationDetails(tenant, false);
            final ActorAware principal = () -> auditor;
            final Collection<? extends GrantedAuthority> grantedAuthorities =
                    Stream.of(authorities).map(SimpleGrantedAuthority::new).toList();
            ctx.setAuthentication(new Authentication() {

                @Override
                public Object getPrincipal() {
                    return principal;
                }

                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return grantedAuthorities;
                }

                @Override
                public boolean isAuthenticated() {
                    return true;
                }

                @Override
                public Object getDetails() {
                    return details;
                }

                @Override
                public Object getCredentials() {
                    return null;
                }

                @Override
                public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getName() {
                    return auditor;
                }
            });
            return ctx;
        }
    }

    /**
     * An implementation of the Spring's {@link Authentication} object which is used within a system security code block and
     * wraps the original auth object. The wrapped object contains the necessary {@link SpRole#SYSTEM_ROLE}
     * which is allowed to execute all secured methods.
     */
    static final class SystemCodeAuthentication implements Authentication {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final List<SimpleGrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority(SpRole.SYSTEM_ROLE));

        private final TenantAwareAuthenticationDetails details;
        private final TenantAwareUser principal;

        private SystemCodeAuthentication(final String tenant) {
            details = new TenantAwareAuthenticationDetails(tenant, false);
            principal = new TenantAwareUser(SYSTEM_ACTOR, SYSTEM_ACTOR, AUTHORITIES, tenant);
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

        private AuthenticationDelegate(final String tenant, final String username, final Authentication delegate) {
            this.delegate = delegate;
            principal = new TenantAwareUser(username, username, delegate != null ? delegate.getAuthorities() : Collections.emptyList(), tenant);
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
            return principal.getAuthorities();
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