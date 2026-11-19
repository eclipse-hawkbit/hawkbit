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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Management API requested tenant - tenant path (/{tenant}/rest/**) and tenant prefixed basic auth user (tenant\\user)
 */
@TestPropertySource(properties = {
        "hawkbit.security.user.rtacme.tenant=" + MgmtTenantResolverTest.TENANT,
        "hawkbit.security.user.rtacme.password={noop}" + MgmtTenantResolverTest.PASSWORD,
        "hawkbit.security.user.rtacme.roles[0]=TENANT_ADMIN",
        "hawkbit.security.user.rtother.tenant=" + MgmtTenantResolverTest.OTHER_TENANT,
        "hawkbit.security.user.rtother.password={noop}" + MgmtTenantResolverTest.PASSWORD,
        "hawkbit.security.user.rtother.roles[0]=TENANT_ADMIN",
        "spring.security.user.name=" + MgmtTenantResolverTest.GLOBAL_USER,
        "spring.security.user.password=" + MgmtTenantResolverTest.PASSWORD })
class MgmtTenantResolverTest extends AbstractHttpSecurityTest {

    static final String TENANT = "RTACME";
    static final String OTHER_TENANT = "RTOTHER";
    static final String PASSWORD = "secret";
    static final String GLOBAL_USER = "rtglobal";

    private static final String USER = "rtacme";
    private static final String OTHER_USER = "rtother";
    private static final String TARGETS = "/rest/v1/targets";
    private static final String CONTROLLER_ID = "requestedTenantTarget";

    @BeforeEach
    void createTarget() throws Exception {
        // the first authenticated request implicitly creates the tenant. on next calls the target already exists
        final HttpResponse<String> response = post(TARGETS, basicAuth(USER, PASSWORD),
                "[{\"controllerId\":\"" + CONTROLLER_ID + "\",\"name\":\"" + CONTROLLER_ID + "\"}]");
        assertThat(response.statusCode()).isIn(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
    }

    /**
     * /{tenant}/rest/** is served as /rest/** and the response links contain the tenant path.
     */
    @Test
    void tenantPathIsForwardedAndLinksContainTenantPath() throws Exception {
        final HttpResponse<String> response = get("/" + TENANT + TARGETS, basicAuth(USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body())
                .contains(CONTROLLER_ID)
                .contains(baseUrl() + "/" + TENANT + TARGETS + "/" + CONTROLLER_ID)
                .doesNotContain("\"" + baseUrl() + TARGETS);
    }

    /**
     * The tenant in the path is case-insensitive (as tenants are) - and is kept as requested in the links.
     */
    @Test
    void tenantPathIsCaseInsensitive() throws Exception {
        final String tenant = TENANT.toLowerCase();
        final HttpResponse<String> response = get("/" + tenant + TARGETS, basicAuth(USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains(baseUrl() + "/" + tenant + TARGETS + "/" + CONTROLLER_ID);
    }

    /**
     * The links of all response types contain the tenant path, e.g. the values of a map.
     */
    @Test
    void mapResponseLinksContainTenantPath() throws Exception {
        final HttpResponse<String> response = get("/" + TENANT + "/rest/v1/system/configs", basicAuth(USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body())
                .contains(baseUrl() + "/" + TENANT + "/rest/v1/system/configs/")
                .doesNotContain("\"" + baseUrl() + "/rest/");
    }

    /**
     * Not normalized tenant paths are rejected (by the firewall) - not served for the normalized path.
     */
    @Test
    void notNormalizedTenantPathIsRejected() throws Exception {
        for (final String path : new String[] {
                "/" + TENANT + "/rest/../actuator/health", "/" + TENANT + "/rest/../../x", "/" + TENANT + "/rest/v1/./targets",
                "/" + TENANT + "/rest/v1;x=y/targets" }) {
            assertThat(get(path, basicAuth(USER, PASSWORD)).statusCode()).as(path).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }
    }

    /**
     * Plain /rest/** links are not changed.
     */
    @Test
    void plainRestLinksAreNotChanged() throws Exception {
        for (final String authorization : new String[] { basicAuth(USER, PASSWORD), basicAuth(TENANT + "\\" + USER, PASSWORD) }) {
            final HttpResponse<String> response = get(TARGETS, authorization);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.body())
                    .contains(baseUrl() + TARGETS + "/" + CONTROLLER_ID)
                    .doesNotContain("/" + TENANT + TARGETS);
        }
    }

    /**
     * Tenant prefixed basic auth user matching the tenant path is accepted (the prefix is removed).
     */
    @Test
    void tenantPathWithTenantPrefixedBasicAuth() throws Exception {
        assertThat(get("/" + TENANT + TARGETS, basicAuth(TENANT + "\\" + USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * User of another tenant is unauthorized - via tenant path or tenant prefixed basic auth user.
     */
    @Test
    void userOfAnotherTenantIsUnauthorized() throws Exception {
        assertUnauthorized(get("/" + OTHER_TENANT + TARGETS, basicAuth(USER, PASSWORD)));
        assertThat(get(TARGETS, basicAuth(OTHER_TENANT + "\\" + USER, PASSWORD)).statusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        // other tenant user doesn't see the target
        final HttpResponse<String> response = get("/" + OTHER_TENANT + TARGETS, basicAuth(OTHER_USER, PASSWORD));
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).doesNotContain(CONTROLLER_ID);
    }

    /**
     * A tenant prefixed basic auth user of another tenant doesn't override the tenant path. The prefix isn't removed (so the user is
     * unknown), and the tenant isn't resolved from the prefix as on /rest/** - otherwise the other tenant user would be authenticated
     * in its own tenant.
     */
    @Test
    void tenantPrefixNotMatchingTenantPathDoesNotOverrideIt() throws Exception {
        assertThat(get("/" + TENANT + TARGETS, basicAuth(OTHER_TENANT + "\\" + OTHER_USER, PASSWORD)).statusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        // while the same user is accepted on plain /rest/** - the prefix requests its own tenant there
        assertThat(get(TARGETS, basicAuth(OTHER_TENANT + "\\" + OTHER_USER, PASSWORD)).statusCode())
                .isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Invalid tenant (path or tenant prefixed basic auth user) is unauthorized.
     */
    @Test
    void invalidTenantIsUnauthorized() throws Exception {
        assertUnauthorized(get("/inv%41lid" + TARGETS, basicAuth(USER, PASSWORD)));
        assertUnauthorized(get("/inv%41lid" + TARGETS, null));
        assertUnauthorized(get(TARGETS, basicAuth("in valid\\" + USER, PASSWORD)));
        // the part before the first backslash is always the tenant
        assertUnauthorized(get(TARGETS, basicAuth("corp/example\\" + USER, PASSWORD)));
    }

    /**
     * Not authenticated tenant path request is unauthorized.
     */
    @Test
    void notAuthenticatedIsUnauthorized() throws Exception {
        assertThat(get("/" + TENANT + TARGETS, null).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(get("/" + TENANT + TARGETS, basicAuth(USER, "wrong")).statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * Global (not tenant scoped) user doesn't get the requested tenant, if not explicitly enabled.
     */
    @Test
    void globalUserDoesNotGetRequestedTenantByDefault() throws Exception {
        final HttpResponse<String> response = get("/" + TENANT + TARGETS, basicAuth(GLOBAL_USER, PASSWORD));
        // behaves as on plain /rest/** (not tenant scoped)
        assertThat(response.statusCode()).isEqualTo(get(TARGETS, basicAuth(GLOBAL_USER, PASSWORD)).statusCode());
        assertThat(response.body()).doesNotContain(CONTROLLER_ID);
    }

    // rejected as for invalid credentials - by the authentication entry point (with WWW-Authenticate)
    private static void assertUnauthorized(final HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).isPresent();
    }
}
