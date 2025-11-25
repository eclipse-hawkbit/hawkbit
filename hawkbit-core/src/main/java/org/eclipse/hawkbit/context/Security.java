/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link Security} provides means for getting the current context (via {@link #currentSecurityContext()}) and then
 * to execute a {@link Runnable} or a {@link Function} in the same context using {@link #withSecurityContext(String, Runnable)}
 * or {@link #withSecurityContext(String, Function, Object)}.
 * <p/>
 * This is useful for scheduled background operations like rollouts and auto assignments where they shall
 * be processed in the scope of the creator.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Security {

    private static SecurityContextSerializer securityContextSerializer = SecurityContextSerializer.JSON_SERIALIZATION;

    /**
     * Provides means to set a custom {@link SecurityContextSerializer} implementation (e.g. {@link SecurityContextSerializer.Nop}.
     * @param serializer the serializer to set. Must not be <code>null</code>.
     */
    public static void setSecurityContextSerializer(@NonNull  final SecurityContextSerializer serializer) {
        securityContextSerializer = Objects.requireNonNull(serializer);
    }

    /**
     * Return the current context encoded as a {@link String}. Depending on the implementation it could,
     * for instance, be a serialized context or a reference to such.
     *
     * @return could be empty if there is nothing to serialize or context aware is not supported.
     */
    public static Optional<String> currentSecurityContext() {
        return Optional.ofNullable(SecurityContextHolder.getContext()).map(securityContextSerializer::serialize);
    }

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param serializedContext created by {@link #currentSecurityContext()}. Must be non-<code>null</code>.
     * @param function function to call in the reconstructed context. Must be non-<code>null</code>.
     * @param t the argument that will be passed to the function
     * @return the function result
     */
    public static <T, R> R withSecurityContext(final String serializedContext, final Function<T, R> function, final T t) {
        Objects.requireNonNull(serializedContext);
        Objects.requireNonNull(function);
        final SecurityContext securityContext = securityContextSerializer.deserialize(serializedContext);
        Objects.requireNonNull(securityContext);

        return withSecurityContext(securityContext, () -> function.apply(t));
    }

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     *
     * @param serializedContext created by {@link #currentSecurityContext()}. Must be non-<code>null</code>.
     * @param runnable runnable to call in the reconstructed context. Must be non-<code>null</code>.
     */
    public static void withSecurityContext(final String serializedContext, final Runnable runnable) {
        Objects.requireNonNull(runnable);
        withSecurityContext(serializedContext, v -> {
            runnable.run();
            return null;
        }, null);
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

    // serializer for security contexts used for background tasks (processing auto assignments and rollouts)
    // the user context is serialized on task creation and then is deserialized and applied when task is executed later
    // it have to be se to {@link Security#setSecurityContextSerializer(SecurityContextSerializer)}
    public interface SecurityContextSerializer {

        /**
         * Serializer that do not serialize (returns null on {@link #serialize(SecurityContext)}) and
         * throws exception on {@link #deserialize(String)}.
         */
        SecurityContextSerializer NOP = new Nop();
        /**
         * Serializer the uses JSON serialization.
         */
        SecurityContextSerializer JSON_SERIALIZATION = new JsonSerialization();

        /**
         * Return security context as string (could be just a reference)
         *
         * @param securityContext the security context
         * @return the securityContext as string
         */
        String serialize(SecurityContext securityContext);

        /**
         * Deserialize security context
         *
         * @param securityContextString string representing the security context
         * @return deserialized security context
         */
        SecurityContext deserialize(String securityContextString);

        /**
         * Empty implementation. Could be used if the serialization shall not be used.
         * It returns <code>null</code> as serialized context and throws exception if
         * someone try to deserialize anything.
         */
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        class Nop implements SecurityContextSerializer {

            @Override
            public String serialize(final SecurityContext securityContext) {
                return null;
            }

            @Override
            public SecurityContext deserialize(final String securityContextString) {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Implementation based on the java serialization.
         */
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @SuppressWarnings("java:S112") // accepted
        class JsonSerialization implements SecurityContextSerializer {

            private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

            @Override
            public String serialize(final SecurityContext securityContext) {
                Objects.requireNonNull(securityContext);
                try {
                    return OBJECT_MAPPER.writeValueAsString(new JsonSerialization.SecCtxInfo(securityContext));
                } catch (final JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SecurityContext deserialize(final String securityContextString) {
                Objects.requireNonNull(securityContextString);
                final String securityContextTrimmed = securityContextString.trim();
                try {
                    return OBJECT_MAPPER.readerFor(JsonSerialization.SecCtxInfo.class).<JsonSerialization.SecCtxInfo> readValue(securityContextTrimmed).toSecurityContext();
                } catch (final JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
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

                SecCtxInfo(final SecurityContext securityContext) {
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
                    auditor = Auditor.resolve(authentication);
                    authorities = authentication.getAuthorities().stream().map(Object::toString).toArray(String[]::new);
                }

                // TODO - remove it in future
                // auditor alias, allows setting for auditor also as username (so supported auditor/username in json)
                @JsonSetter("username")
                private void setUsername(final String username) {
                    this.auditor = username;
                }

                private SecurityContext toSecurityContext() {
                    final SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                    final Object details = tenant == null ? null : new TenantAwareAuthenticationDetails(tenant, false);
                    final Auditor.AuditorAwarePrincipal principal = () -> auditor;
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
        }

        /**
         * Implementation based on the java serialization.
         */
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        @SuppressWarnings("java:S112") // accepted
        class JavaSerialization implements SecurityContextSerializer {

            @Override
            public String serialize(final SecurityContext securityContext) {
                Objects.requireNonNull(securityContext);
                try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(securityContext);
                    oos.flush();
                    return Base64.getEncoder().encodeToString(baos.toByteArray());
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SecurityContext deserialize(final String securityContextString) {
                Objects.requireNonNull(securityContextString);
                try (final ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(securityContextString));
                        final ObjectInputStream ois = new ObjectInputStream(bais)) {
                    return (SecurityContext) ois.readObject();
                } catch (final IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}