/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.app.dmf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Smoke test that the DMF server application context starts.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dmf-start;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database=H2",
        "spring.flyway.enabled=true" })
class DmfStartTest {

    @Test
    void contextLoads() {
        // the test passes if the application context starts
    }
}
