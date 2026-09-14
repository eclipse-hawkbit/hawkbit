/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.hawkbit.throttle.TenantThrottle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test proving {@code @Transactional(propagation = Propagation.REQUIRES_NEW)} uses reentrancy
 * bypass: outer + inner transactions hold 2 real DB connections but only 1 throttle permit, preventing
 * self-deadlock when the tenant is at its fair-share limit.
 */
@EnableTransactionManagement
class ThrottlingRequiresNewTransactionTest {

    private static final ToIntFunction<String> UNLIMITED = tenant -> -1;

    private HikariDataSource rawDataSource;
    private ThrottlingDataSourceDecorator throttledDataSource;
    private TenantThrottle throttle;
    private PlatformTransactionManager txManager;

    @BeforeEach
    void setUp() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:requires_new_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(10);
        rawDataSource = new HikariDataSource(config);

        throttle = new TenantThrottle(1, 0, UNLIMITED); // single permit per tenant
        throttledDataSource = new ThrottlingDataSourceDecorator(rawDataSource, throttle, Duration.ofSeconds(1));
        txManager = new DataSourceTransactionManager(throttledDataSource);
    }

    @AfterEach
    void tearDown() {
        rawDataSource.close();
    }

    @Test
    void requiresNewHoldsTwoConnectionsButOnePermit() {
        final AtomicInteger activeConnections = new AtomicInteger();
        final AtomicInteger maxConnections = new AtomicInteger();

        asTenant("acme", () -> {
            final TransactionTemplate outer = new TransactionTemplate(txManager);
            outer.setPropagationBehavior(Propagation.REQUIRED.value());

            outer.executeWithoutResult(status -> {
                assertThat(throttle.inUse("acme")).isEqualTo(1); // outer took 1 permit
                activeConnections.set(rawDataSource.getHikariPoolMXBean().getActiveConnections());
                assertThat(activeConnections.get()).isEqualTo(1); // 1 real connection from pool

                final TransactionTemplate inner = new TransactionTemplate(txManager);
                inner.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

                inner.executeWithoutResult(innerStatus -> {
                    // reentrancy bypass: no second permit acquired
                    assertThat(throttle.inUse("acme")).isEqualTo(1);

                    // BUT both outer + inner hold real connections from HikariCP
                    activeConnections.set(rawDataSource.getHikariPoolMXBean().getActiveConnections());
                    maxConnections.set(Math.max(maxConnections.get(), activeConnections.get()));
                    assertThat(activeConnections.get()).isEqualTo(2); // 2 real connections active
                });

                // inner closed, back to 1 connection
                activeConnections.set(rawDataSource.getHikariPoolMXBean().getActiveConnections());
                assertThat(activeConnections.get()).isEqualTo(1);
                assertThat(throttle.inUse("acme")).isEqualTo(1); // still 1 permit
            });

            // outer closed, permit released
            assertThat(throttle.inUse("acme")).isZero();
        });

        // verify we actually reached 2 concurrent connections during inner tx
        assertThat(maxConnections.get()).isEqualTo(2);
    }

    @Test
    void requiresNewDoesNotDeadlockWhenTenantAtLimit() {
        // tenant at limit (1 permit cap), outer holds it
        asTenant("acme", () -> {
            final TransactionTemplate outer = new TransactionTemplate(txManager);
            outer.setPropagationBehavior(Propagation.REQUIRED.value());

            outer.executeWithoutResult(status -> {
                assertThat(throttle.inUse("acme")).isEqualTo(1);

                // REQUIRES_NEW on same thread → reentrancy bypass, no deadlock
                final TransactionTemplate inner = new TransactionTemplate(txManager);
                inner.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

                inner.executeWithoutResult(innerStatus -> {
                    // would deadlock if it tried to acquire second permit (cap=1, outer holds 1)
                    assertThat(throttle.inUse("acme")).isEqualTo(1); // bypassed
                });
            });
        });
    }
}
