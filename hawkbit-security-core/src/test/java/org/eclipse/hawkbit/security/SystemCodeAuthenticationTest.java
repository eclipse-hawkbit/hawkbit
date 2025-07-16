/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SystemCodeAuthenticationTest {

    private static final SystemSecurityContext SYSTEM_SECURITY_CONTEXT = new SystemSecurityContext(new TenantAware() {

        @Override
        public String getCurrentTenant() {
            return "tenant";
        }

        @Override
        public String getCurrentUsername() {
            return "user";
        }

        @Override
        public <T> T runAsTenant(final String tenant, final TenantRunner<T> tenantRunner) {
            return tenantRunner.run();
        }

        @Override
        public <T> T runAsTenantAsUser(final String tenant, final String username, final TenantRunner<T> tenantRunner) {
            return tenantRunner.run();
        }
    });

    @Test
    void testSerializationWithoutNull() {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test", "pass", List.of(new SimpleGrantedAuthority("anonymous")));
        auth.setDetails("string details");
        test(auth);
    }

    @Test
    void testSerializationWithNullPrincipal() {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, "pass", List.of(new SimpleGrantedAuthority("anonymous")));
        auth.setDetails("string details");
        test(auth);
    }

    @Test
    void testSerializationWithNullCredentials() {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test", null, List.of(new SimpleGrantedAuthority("anonymous")));
        auth.setDetails("string details");
        test(auth);
    }

    @Test
    void testSerializationWithNullDetails() {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test", "pass", List.of(new SimpleGrantedAuthority("anonymous")));
        auth.setDetails(null);
        test(auth);
    }

    @Test
    void testSerializationWitAllNull() {
        final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(null, null, List.of(new SimpleGrantedAuthority("anonymous")));
        auth.setDetails(null);
        test(auth);
    }

    private static void test(final UsernamePasswordAuthenticationToken auth) {
        final SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
        SYSTEM_SECURITY_CONTEXT.runAsSystemAsTenant(() -> {
            final Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            Assertions.assertThat(currentAuth.getClass().getSimpleName()).isEqualTo("SystemCodeAuthentication");
            Assertions.assertThat(currentAuth.getPrincipal()).isEqualTo(auth.getPrincipal());
            Assertions.assertThat(currentAuth.getCredentials()).isEqualTo(auth.getCredentials());
            Assertions.assertThat(currentAuth.getAuthorities()).isEqualTo(List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_CODE")));
            Assertions.assertThat(currentAuth.getDetails()).isEqualTo(auth.getDetails());

            final Authentication serializedAndDeserializedAuth = serializeAndDeserialize(currentAuth);
            Assertions.assertThat(serializedAndDeserializedAuth.getClass().getSimpleName()).isEqualTo("SystemCodeAuthentication");
            Assertions.assertThat(serializedAndDeserializedAuth.getPrincipal()).isEqualTo(auth.getPrincipal());
            Assertions.assertThat(serializedAndDeserializedAuth.getCredentials()).isEqualTo(auth.getCredentials());
            Assertions.assertThat(serializedAndDeserializedAuth.getAuthorities()).isEqualTo(List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_CODE")));
            Assertions.assertThat(serializedAndDeserializedAuth.getDetails()).isEqualTo(auth.getDetails());
            return null;
        }, "tenant");
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static <T> T serializeAndDeserialize(final T object) throws IOException, ClassNotFoundException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(object);
                oos.flush();
            }
            try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                return (T) ois.readObject();
            }
        }
    }
}