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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end proof of the per-tenant DB connection throttle over real HTTP (MockMvc against the fully
 * booted update-server, {@code two-layer} fast-reject): with a single-permit-per-tenant cap, concurrent
 * requests for the same tenant exceed its fair share and are shed with HTTP 429 + {@code Retry-After},
 * while the holder is served — exercising marker filter → DataSource decorator → gate → engine → 429.
 * <p/>
 * Run: {@code mvn test -pl :hawkbit-update-server -am -Dtest=ThrottleConnectionPoolE2ETest}.
 */
@SpringBootTest(properties = {
        "hawkbit.throttle.enabled=true",
        "hawkbit.throttle.limit=1",       // each tenant may hold one connection at a time
        "hawkbit.throttle.timeout=0",     // fast-reject
        "hawkbit.throttle.capacity=4" })
class ThrottleConnectionPoolE2ETest extends AbstractSecurityTest {

    @Test
    void concurrentRequestsForOneTenantAreThrottledWith429() throws Exception {
        // warm-up: one sequential request creates the tenant + default types (avoids a cold-start write
        // race) and proves a lone request is served (not throttled)
        mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        final int concurrency = 8;
        final ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        final CountDownLatch ready = new CountDownLatch(concurrency);
        final CountDownLatch go = new CountDownLatch(1);

        try {
            final List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "2000")
                            .with(httpBasic("admin", "admin"))).andReturn();
                }));
            }

            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            final List<Integer> statuses = new ArrayList<>();
            final List<MvcResult> results = new ArrayList<>();
            for (final Future<MvcResult> future : futures) {
                final MvcResult result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
                statuses.add(result.getResponse().getStatus());
            }

            assertThat(statuses).as("at least one request should be served").contains(200);
            assertThat(statuses).as("over-share concurrent requests should be shed with 429").contains(429);
            results.stream()
                    .filter(result -> result.getResponse().getStatus() == 429)
                    .forEach(result -> assertThat(result.getResponse().getHeader("Retry-After"))
                            .as("429 responses must carry a Retry-After hint").isEqualTo("1"));
        } finally {
            pool.shutdownNow();
        }
    }
}
