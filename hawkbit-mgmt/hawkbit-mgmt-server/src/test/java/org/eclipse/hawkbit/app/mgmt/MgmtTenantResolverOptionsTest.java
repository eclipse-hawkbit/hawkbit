/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.mgmt;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Management API requested tenant - global user opt-in, disabled tenant prefixed basic auth user and JWT tenant check
 */
@TestPropertySource(properties = {
        "hawkbit.security.user.rtoacme.tenant=" + MgmtTenantResolverOptionsTest.TENANT,
        "hawkbit.security.user.rtoacme.password={noop}" + MgmtTenantResolverOptionsTest.PASSWORD,
        "hawkbit.security.user.rtoacme.roles[0]=TENANT_ADMIN",
        "spring.security.user.name=" + MgmtTenantResolverOptionsTest.GLOBAL_USER,
        "spring.security.user.password=" + MgmtTenantResolverOptionsTest.PASSWORD,
        "hawkbit.server.security.tenant-resolver.global-user-requested-tenant=true",
        "hawkbit.server.security.tenant-resolver.tenant-prefixed-basic-auth=false",
        "hawkbit.server.security.oauth2.resourceserver.enabled=true",
        "hawkbit.server.security.oauth2.resourceserver.jwt.claim.tenant=tenant",
        "hawkbit.server.security.allow-http-basic-on-o-auth-enabled=true" })
class MgmtTenantResolverOptionsTest extends AbstractHttpSecurityTest {

    static final String TENANT = "RTOACME";
    static final String PASSWORD = "secret";
    static final String GLOBAL_USER = "rtoglobal";

    private static final String USER = "rtoacme";
    private static final String TARGETS = "/rest/v1/targets";
    private static final String CONTROLLER_ID = "requestedTenantOptionsTarget";

    @BeforeEach
    void createTarget() throws Exception {
        // the first authenticated request implicitly creates the tenant. on next calls the target already exists
        final HttpResponse<String> response = post(TARGETS, basicAuth(USER, PASSWORD),
                "[{\"controllerId\":\"" + CONTROLLER_ID + "\",\"name\":\"" + CONTROLLER_ID + "\"}]");
        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
    }

    /**
     * Global user acts in the scope of the requested tenant, if enabled.
     */
    @Test
    void globalUserGetsRequestedTenant() throws Exception {
        final HttpResponse<String> response = get("/" + TENANT + TARGETS, basicAuth(GLOBAL_USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(CONTROLLER_ID);
    }

    /**
     * Global user acting in the requested tenant doesn't create a not existing tenant (e.g. on a typo) - it is unauthorized.
     */
    @Test
    void globalUserDoesNotCreateRequestedTenant() throws Exception {
        final String notExistingTenantPath = "/RTOACME_TYPO" + TARGETS;
        final HttpResponse<String> response = get(notExistingTenantPath, basicAuth(GLOBAL_USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).isPresent();
        // still not existing on the next call
        assertThat(get(notExistingTenantPath, basicAuth(GLOBAL_USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * Tenant prefixed basic auth user on /rest/** is not handled, if disabled - so the user is unknown.
     */
    @Test
    void tenantPrefixedBasicAuthDisabled() throws Exception {
        assertThat(get(TARGETS, basicAuth(TENANT + "\\" + USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        // still handled on the tenant path
        assertThat(get("/" + TENANT + TARGETS, basicAuth(TENANT + "\\" + USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * JWT authenticated principal of another tenant is unauthorized.
     */
    @Test
    void jwtOfAnotherTenantIsUnauthorized() throws Exception {
        // the test decoder takes the token value as tenant
        final HttpResponse<String> response = get("/" + TENANT + TARGETS, "Bearer " + TENANT);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(CONTROLLER_ID);
        assertThat(get("/" + TENANT + TARGETS, "Bearer OTHER").statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @TestConfiguration
    static class JwtConfig {

        // decodes (without validation) the token value as the tenant of the JWT principal
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .subject("jwtuser")
                    .claim("preferred_username", "jwtuser")
                    .claim("tenant", token)
                    .claim("roles", List.of("TENANT_ADMIN", "READ_TARGET"))
                    .build();
        }
    }
}
