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
 * End-to-end proof of per-tenant isolation: while tenant A saturates its own share of the pool (and is
 * shed with 429), tenant B is unaffected and served. This is the core fairness guarantee — one tenant
 * cannot starve another. Two static users in two tenants; A storms, B probes concurrently.
 */
@SpringBootTest(properties = {
        "hawkbit.throttle.enabled=true",
        "hawkbit.throttle.limit=1",
        "hawkbit.throttle.timeout=0",
        "hawkbit.throttle.capacity=4",
        // second tenant/user (operator-configured static user)
        "hawkbit.security.user.tenanttwo.tenant=TENANT_TWO",
        "hawkbit.security.user.tenanttwo.password={noop}pw",
        "hawkbit.security.user.tenanttwo.roles=TENANT_ADMIN" })
class ThrottleFairnessE2ETest extends AbstractSecurityTest {

    @Test
    void oneTenantStormDoesNotStarveAnother() throws Exception {
        // warm up both tenants (create tenant + default types) and prove lone requests are served
        mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
        mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0").with(httpBasic("tenanttwo", "pw")))
                .andExpect(status().isOk());

        final int stormSize = 6;
        final ExecutorService pool = Executors.newFixedThreadPool(stormSize);
        final CountDownLatch go = new CountDownLatch(1);
        try {
            // tenant A (admin) storm: each request holds its single permit for 1.5s
            final List<Future<MvcResult>> aStorm = new ArrayList<>();
            for (int i = 0; i < stormSize; i++) {
                aStorm.add(pool.submit(() -> {
                    go.await();
                    return mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "1500")
                            .with(httpBasic("admin", "admin"))).andReturn();
                }));
            }
            go.countDown();
            Thread.sleep(300); // let an A request grab and hold tenant A's permit

            // tenant B probes while A is saturated — B has its own permit, must be served
            final int b1 = mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0")
                    .with(httpBasic("tenanttwo", "pw"))).andReturn().getResponse().getStatus();
            final int b2 = mvc.perform(get(ThrottleTestSlowEndpoint.SLOW_PATH).param("ms", "0")
                    .with(httpBasic("tenanttwo", "pw"))).andReturn().getResponse().getStatus();

            final List<Integer> aStatuses = new ArrayList<>();
            for (final Future<MvcResult> future : aStorm) {
                aStatuses.add(future.get(30, TimeUnit.SECONDS).getResponse().getStatus());
            }

            assertThat(aStatuses).as("tenant A over-share requests are shed").contains(429);
            assertThat(b1).as("tenant B served despite tenant A's storm").isEqualTo(200);
            assertThat(b2).as("tenant B served despite tenant A's storm").isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }
    }
}
