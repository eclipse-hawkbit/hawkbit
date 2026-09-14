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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.repository.test.util.SharedSqlTestDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end proof that the throttle works under <b>real virtual threads</b>: a full Tomcat on a random
 * port with {@code spring.threads.virtual.enabled=true}, driven over real HTTP. Each request runs on a
 * virtual thread; over-share requests block on the permit (db-centric bounded wait) and are served as it
 * frees. That the run completes (no hang) confirms the engine's {@code ReentrantLock} does not pin the
 * carrier thread — the correctness precondition for the db-centric prod mode. (A formal pinning audit
 * via {@code -Djdk.tracePinnedThreads}/JFR under load is still a separate staging step.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.threads.virtual.enabled=true",
        "hawkbit.throttle.enabled=true",
        "hawkbit.throttle.limit=1",
        "hawkbit.throttle.timeout=5s",
        "hawkbit.throttle.capacity=4" })
@ExtendWith(SharedSqlTestDatabaseExtension.class)
class ThrottleVirtualThreadsE2ETest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void throttleServesQueuedRequestsUnderRealVirtualThreads() throws Exception {
        get(0); // warm up the tenant

        final int concurrency = 5; // 5 x 300ms serialized through one permit = ~1.5s < 5s timeout
        final ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        final CountDownLatch go = new CountDownLatch(1);
        try {
            final List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return get(300);
                }));
            }
            go.countDown();

            final List<Integer> statuses = new ArrayList<>();
            for (final Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(statuses).as("under real virtual threads, queued requests are all served (no hang, no 429)")
                    .containsOnly(200);
        } finally {
            pool.shutdownNow();
        }
    }

    private int get(final long holdMs) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + ThrottleTestSlowEndpoint.SLOW_PATH + "?ms=" + holdMs))
                .header("Authorization", AUTH)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
