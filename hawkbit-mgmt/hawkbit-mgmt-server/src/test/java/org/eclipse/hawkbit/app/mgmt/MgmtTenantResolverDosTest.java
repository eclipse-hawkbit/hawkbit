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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Feature: Integration Test - Security<br/>
 * Story: Management API DoS filter applies to the tenant path (/{tenant}/rest/**)
 */
@TestPropertySource(properties = {
        "hawkbit.security.user.rtdacme.tenant=" + MgmtTenantResolverDosTest.TENANT,
        "hawkbit.security.user.rtdacme.password={noop}" + MgmtTenantResolverDosTest.PASSWORD,
        "hawkbit.security.user.rtdacme.roles[0]=TENANT_ADMIN",
        // localhost is whitelisted by default
        "hawkbit.server.security.dos.filter.whitelist=",
        "hawkbit.server.security.dos.filter.max-read=1" })
// ordered - the client (localhost) is throttled after each test, so the test expecting a not throttled first request runs first
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MgmtTenantResolverDosTest extends AbstractHttpSecurityTest {

    static final String TENANT = "RTDACME";
    static final String PASSWORD = "secret";

    private static final String USER = "rtdacme";

    /**
     * The tenant path request (not matched by a /rest/* servlet url pattern) is counted once and is throttled.
     */
    @Test
    @Order(1)
    void tenantPathIsCountedOnceAndThrottled() throws Exception {
        final String path = "/" + TENANT + "/rest/v1/targets";
        // max one read per second - if counted twice the first request would be rejected
        assertThat(get(path, basicAuth(USER, PASSWORD)).statusCode()).isEqualTo(HttpStatus.OK.value());

        final List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            statuses.add(get(path, basicAuth(USER, PASSWORD)).statusCode());
        }
        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    /**
     * A percent-encoded path (decoded by the servlet container) is counted and throttled too.
     */
    @Test
    @Order(2)
    void percentEncodedPathIsThrottled() throws Exception {
        for (final String path : new String[] { "/r%65st/v1/targets", "/" + TENANT + "/r%65st/v1/targets" }) {
            final List<Integer> statuses = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                statuses.add(get(path, basicAuth(USER, PASSWORD)).statusCode());
            }
            assertThat(statuses).as(path).contains(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }
}
