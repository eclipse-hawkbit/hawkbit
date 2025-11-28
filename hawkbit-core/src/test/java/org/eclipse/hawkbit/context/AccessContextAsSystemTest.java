/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.context.AccessContext.asSystemAsTenant;

import java.util.List;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class AccessContextAsSystemTest {

    @Test
    void test() {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test", "pass", List.of(new SimpleGrantedAuthority("anonymous")));
        authentication.setDetails("string details");
        test(authentication);
    }

    @Test
    void testWithNullPrincipal() {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                null, "pass", List.of(new SimpleGrantedAuthority("anonymous")));
        authentication.setDetails("string details");
        test(authentication);
    }

    @Test
    void testWithNullCredentials() {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test", null, List.of(new SimpleGrantedAuthority("anonymous")));
        authentication.setDetails("string details");
        test(authentication);
    }

    @Test
    void testWitAllNull() {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                null, null, List.of(new SimpleGrantedAuthority("anonymous")));
        authentication.setDetails(null);
        test(authentication);
    }

    private static void test(final UsernamePasswordAuthenticationToken authentication) {
        final SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(authentication);
        SecurityContextHolder.setContext(sc);
        asSystemAsTenant("tenant", () -> {
            final Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(currentAuth.getClass().getSimpleName()).isEqualTo("SystemCodeAuthentication");
            assertThat(currentAuth.getCredentials()).isNull();
            assertThat(currentAuth.getAuthorities()).isEqualTo(List.of(new SimpleGrantedAuthority(SpRole.SYSTEM_ROLE)));
            assertThat(currentAuth.getDetails()).isEqualTo(new TenantAwareAuthenticationDetails("tenant", false));
        });
        SecurityContextHolder.clearContext();
    }
}