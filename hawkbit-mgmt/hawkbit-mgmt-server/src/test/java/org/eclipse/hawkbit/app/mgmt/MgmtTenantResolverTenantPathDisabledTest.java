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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Management API requested tenant - disabled tenant path (/{tenant}/rest/**)
 */
@TestPropertySource(properties = {
        "hawkbit.security.user.rtpacme.tenant=" + MgmtTenantResolverTenantPathDisabledTest.TENANT,
        "hawkbit.security.user.rtpacme.password={noop}" + MgmtTenantResolverTenantPathDisabledTest.PASSWORD,
        "hawkbit.security.user.rtpacme.roles[0]=TENANT_ADMIN",
        "hawkbit.server.security.tenant-resolver.tenant-path=false" })
class MgmtTenantResolverTenantPathDisabledTest extends AbstractHttpSecurityTest {

    static final String TENANT = "RTPACME";
    static final String PASSWORD = "secret";

    private static final String USER = "rtpacme";
    private static final String TARGETS = "/rest/v1/targets";

    /**
     * The tenant path isn't served as /rest/** if disabled, while the tenant prefixed basic auth user is still handled.
     */
    @Test
    void tenantPathIsNotServed() throws Exception {
        assertThat(get("/" + TENANT + TARGETS, basicAuth(USER, PASSWORD)).statusCode()).isNotEqualTo(HttpStatus.OK.value());
        assertThat(get(TARGETS, basicAuth(USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(get(TARGETS, basicAuth(TENANT + "\\" + USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.OK.value());
    }
}
