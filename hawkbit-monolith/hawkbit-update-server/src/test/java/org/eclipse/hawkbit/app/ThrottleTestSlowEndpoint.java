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

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint shared by the throttle E2E tests (component-scanned from {@code src/test}, so it
 * never ships to production). It borrows a pooled connection through the (throttled) application
 * DataSource and holds it for {@code ms} milliseconds, so concurrent requests overlap deterministically
 * without a DB-specific sleep function. Sits under the Management API path so the tenant marker filter
 * applies. Inert unless throttling is enabled.
 */
@RestController
class ThrottleTestSlowEndpoint {

    static final String SLOW_PATH = MgmtRestConstants.REST + "/throttle-e2e/slow";

    private final DataSource dataSource;

    ThrottleTestSlowEndpoint(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping(MgmtRestConstants.REST + "/throttle-e2e/slow")
    ResponseEntity<String> slow(@RequestParam(name = "ms", defaultValue = "2000") final long ms) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            Thread.sleep(ms);
        }
        return ResponseEntity.ok("ok");
    }
}
