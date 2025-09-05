/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.security;

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
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware.AuditorAwarePrincipal;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

// serializer for security contexts used for background tasks (processing auto assignments and rollouts)
// the user context is serialized on task creation and then is deserialized and applied when task is executed later
public interface SecurityContextSerializer {

    /**
     * Serializer that do not serialize (returns null on {@link #serialize(SecurityContext)}) and
     * throws exception on {@link #deserialize(String)}.
     */
    SecurityContextSerializer NOP = new Nop();
    /**
     * Serializer the uses JSON serialization.
     * <p/>
     * Note that on deserialization this serialization does (if configured) fallback to {@link #JAVA_SERIALIZATION}.
     */
    SecurityContextSerializer JSON_SERIALIZATION = new JsonSerialization();
    /**
     * Serializer the uses Java serialization of {@link java.io.Serializable} objects (legacy, not recommended).
     * <p/>
     * Note that serialized via java serialization context might become unreadable if incompatible
     * changes are made to the object classes.
     */
    SecurityContextSerializer JAVA_SERIALIZATION = new JavaSerialization();

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
    class Nop implements SecurityContextSerializer {

        private Nop() {
        }

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
        private static final boolean FALLBACK_TO_JAVA_SERIALIZATION =
                !Boolean.getBoolean("hawkbit.security.contextSerializer.json.no-fallback-to-java");

        @Override
        public String serialize(final SecurityContext securityContext) {
            Objects.requireNonNull(securityContext);
            try {
                return OBJECT_MAPPER.writeValueAsString(new SecCtxInfo(securityContext));
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public SecurityContext deserialize(final String securityContextString) {
            Objects.requireNonNull(securityContextString);
            final String securityContextTrimmed = securityContextString.trim();
            try {
                // java serialization starts with {@link ObjectStreamConstants#STREAM_MAGIC} (0xAC, 0xED) bytes
                // while trimmed json object starts with '{'
                if (FALLBACK_TO_JAVA_SERIALIZATION &&
                        (securityContextTrimmed.isEmpty() || securityContextTrimmed.charAt(0) != '{')) {
                    return JAVA_SERIALIZATION.deserialize(securityContextString);
                }

                return OBJECT_MAPPER.readerFor(SecCtxInfo.class).<SecCtxInfo> readValue(securityContextTrimmed).toSecurityContext();
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        // simplified info for the security context keeping just the basic info needed for background execution of
        @NoArgsConstructor
        @Data
        private static class SecCtxInfo implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            private String tenant;
            private boolean controller;
            private String auditor;
            private String[] authorities;
            private boolean authenticated;

            SecCtxInfo(final SecurityContext securityContext) {
                final Authentication authentication = securityContext.getAuthentication();
                if (authentication.getDetails() instanceof TenantAwareAuthenticationDetails tenantAwareDetails) {
                    tenant = tenantAwareDetails.tenant();
                    controller = tenantAwareDetails.controller();
                } else {
                    tenant = null;
                    controller = false;
                }
                // keep the auditor, ofr audit purposes,
                // sets principal to the resolved auditor and then deserialized authentication will return it as principal
                // since the class is not known to auditor aware - it shall used default - principal as auditor
                auditor = SpringSecurityAuditorAware.resolveAuditor(authentication);
                authorities = authentication.getAuthorities().stream().map(Object::toString).toArray(String[]::new);
                authenticated = authentication.isAuthenticated();
            }

            private SecurityContext toSecurityContext() {
                final SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                final Object details = tenant == null ? null : new TenantAwareAuthenticationDetails(tenant, controller);
                final AuditorAwarePrincipal principal = () -> auditor;
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
                        return authenticated;
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
