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
import static org.eclipse.hawkbit.context.Security.SecurityContextSerializer.JSON_SERIALIZATION;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextSerializerTest {

    private static final Set<String> AUTHORITIES = SpPermission.getAllAuthorities();

    @Test
    void testJsonSerialization() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final UsernamePasswordAuthenticationToken userPassAuthentication = new UsernamePasswordAuthenticationToken(
                "user", null, AUTHORITIES.stream().map(SimpleGrantedAuthority::new).toList());
        final TenantAwareAuthenticationDetails details = new TenantAwareAuthenticationDetails("my_tenant", false);
        userPassAuthentication.setDetails(details);
        securityContext.setAuthentication(userPassAuthentication);

        final String serialized = JSON_SERIALIZATION.serialize(securityContext);
        final SecurityContext deserialized = JSON_SERIALIZATION.deserialize(serialized);
        final Authentication authentication = deserialized.getAuthentication();
        assertThat(Auditor.resolve(authentication)).hasToString("user");
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .isEqualTo(AUTHORITIES);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getDetails()).isEqualTo(details);
    }

    @Test
    void testJsonSerializationSize() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final UsernamePasswordAuthenticationToken userPassAuthentication = new UsernamePasswordAuthenticationToken(
                "FirstName.FamilyName@domain1.domain0.com",
                Map.of("should not be in" + bigString(10_000), "the output" + bigString(15_000)),
                AUTHORITIES.stream().map(SimpleGrantedAuthority::new).toList());
        final TenantAwareAuthenticationDetails details = new TenantAwareAuthenticationDetails("my_test_enant", false);
        userPassAuthentication.setDetails(details);
        securityContext.setAuthentication(userPassAuthentication);

        final String serialized = JSON_SERIALIZATION.serialize(securityContext);
        assertThat(serialized).hasSizeLessThan(4096); // ensure that it is not too big
    }

    @Test
    void testUsername() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final UsernamePasswordAuthenticationToken userPassAuthentication = new UsernamePasswordAuthenticationToken(
                "user", null, AUTHORITIES.stream().map(SimpleGrantedAuthority::new).toList());
        final TenantAwareAuthenticationDetails details = new TenantAwareAuthenticationDetails("my_tenant", false);
        userPassAuthentication.setDetails(details);
        securityContext.setAuthentication(userPassAuthentication);

        final String serialized = JSON_SERIALIZATION.serialize(securityContext).replace("auditor", "username");
        final SecurityContext deserialized = JSON_SERIALIZATION.deserialize(serialized);
        final Authentication authentication = deserialized.getAuthentication();
        assertThat(Auditor.resolve(authentication)).hasToString("user");
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .isEqualTo(AUTHORITIES);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getDetails()).isEqualTo(details);
    }

    private static String bigString(final int length) {
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + i % 26));
        }
        return sb.toString();
    }
}