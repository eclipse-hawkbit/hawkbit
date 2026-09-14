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
 * End-to-end proof of the db-centric backpressure model: instead of fast-rejecting, over-share requests
 * <em>wait</em> up to {@code connection.timeout} in the fair queue and are served as the permit frees.
 * Contrast with {@link ThrottleConnectionPoolE2ETest} (two-layer, immediate 429). Here concurrent
 * requests are serialized through the single permit and all complete within the timeout — slow, but
 * served, and none get 429.
 */
@SpringBootTest(properties = {
        "hawkbit.throttle.enabled=true",
        "hawkbit.throttle.limit=1",
        "hawkbit.throttle.timeout=5s",  // bounded wait = backpressure
        "hawkbit.throttle.capacity=4" })
class ThrottleWaitThenServeE2ETest extends AbstractSecurityTest {

    @Test
    void concurrentRequestsAreQueuedAndAllServedWithinTheTimeout() throws Exception {
        mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        final int concurrency = 5; // 5 x 300ms serialized = ~1.5s, well under the 5s timeout
        final ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        final CountDownLatch go = new CountDownLatch(1);
        try {
            final List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "300")
                            .with(httpBasic("admin", "admin"))).andReturn();
                }));
            }
            go.countDown();

            final List<Integer> statuses = new ArrayList<>();
            for (final Future<MvcResult> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS).getResponse().getStatus());
            }

            assertThat(statuses).as("db-centric waits then serves: every request completes, none rejected")
                    .containsOnly(200);
        } finally {
            pool.shutdownNow();
        }
    }
}
