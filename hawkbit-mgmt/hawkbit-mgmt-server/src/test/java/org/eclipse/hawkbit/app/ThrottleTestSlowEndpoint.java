/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint shared by the throttle E2E tests (component-scanned from {@code src/test}, so it
 * never ships to production). Executes a DB-native SLEEP query to hold a pooled connection for the
 * specified duration, proving concurrent requests truly overlap and exhaust the connection pool.
 * Sits under the Management API path so the tenant marker filter applies.
 */
@RestController
class ThrottleTestSlowEndpoint {

    static final String SLOW_PATH = MgmtRestConstants.REST + "/throttle-e2e/slow";

    private final EntityManager em;
    private final String sleepQueryTemplate;

    ThrottleTestSlowEndpoint(final EntityManager em,
            @Value("${spring.datasource.url:jdbc:h2:mem:hawkbit}") final String datasourceUrl) {
        this.em = em;
        // Detect DB dialect from datasource URL and use native sleep function
        if (datasourceUrl.contains("mysql")) {
            this.sleepQueryTemplate = "SELECT SLEEP(%d)"; // MySQL: SLEEP(seconds) returns 0
        } else if (datasourceUrl.contains("postgresql")) {
            this.sleepQueryTemplate = "SELECT pg_sleep(%d)"; // PostgreSQL: pg_sleep(seconds) returns void
        } else {
            // H2: no native sleep, use Java Thread.sleep (fallback for tests only)
            this.sleepQueryTemplate = null;
        }
    }

    @Transactional
    @GetMapping(MgmtRestConstants.REST + "/throttle-e2e/slow")
    ResponseEntity<String> slow(@RequestParam(name = "ms", defaultValue = "2000") final long ms) throws Exception {
        if (sleepQueryTemplate != null) {
            // Use DB-native sleep: connection held by query execution for entire duration
            final long seconds = Math.max(1, ms / 1000);
            em.createNativeQuery(String.format(sleepQueryTemplate, seconds)).getSingleResult();
        } else {
            // H2 fallback: SELECT 1 + Thread.sleep inside @Transactional (connection held by tx)
            em.createNativeQuery("SELECT 1").getSingleResult();
            Thread.sleep(ms);
        }
        return ResponseEntity.ok("ok");
    }
}
