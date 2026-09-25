/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.throttle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.function.Consumer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.hawkbit.throttle.Permit;
import org.eclipse.hawkbit.throttle.Throttle;
import org.eclipse.hawkbit.throttle.ThrottledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end verification of the decorator against a real HikariCP pool over H2. The decorator is also where the
 * permit/connection binding lives — root tracking per thread, child permits for nested borrows, release on close — so
 * this is the only place that behaviour is exercised.
 */
class ThrottlingDataSourceDecoratorTest {

    private HikariDataSource target;

    @BeforeEach
    void setUp() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:throttle_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(5);
        target = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        target.close();
    }

    @Test
    void tenantRequestExecutesRealQueryAndReleasesPermitOnClose() {
        final Throttle throttle = throttle(5, config -> { });
        final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);

        asTenant("acme", () -> {
            try (final Connection connection = dataSource.getConnection();
                    final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1); // real query ran through the proxied connection
                assertThat(throttle.stats().inUse("acme")).isEqualTo(1);
            } catch (final SQLException e) {
                throw new IllegalStateException(e);
            }

            assertThat(throttle.stats().inUse("acme")).isZero(); // released when the connection returned to the pool
        });
    }

    @Test
    void workWithoutTenantContextIsChargedToTheNullKey() throws Exception {
        final Throttle throttle = throttle(2, config -> { });
        final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);

        // no tenant context → counted as internal work rather than waved through, so permits mirror pool usage
        try (final Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            assertThat(throttle.stats().inUse(null)).isEqualTo(1);
        }
        assertThat(throttle.stats().inUse()).isZero();
    }

    @Test
    void nestedBorrowOnSameThreadTakesAChildPermit() throws Exception {
        // regression: a closing nested connection must not deregister the outer one as the thread's root permit,
        // which would make the next borrow a second root and double-count the unit of work
        final Throttle throttle = throttle(4, config -> { });
        final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);

        final Connection outer = dataSource.getConnection();
        for (int i = 0; i < 3; i++) {
            final Connection nested = dataSource.getConnection(); // outer still open → child permit
            assertThat(throttle.stats().inUse()).isEqualTo(2);
            assertThat(activeConnections()).isEqualTo(2); // permits and real connections agree
            assertThat(throttle.stats().units()).isEqualTo(1); // still one unit of work

            nested.close();
            assertThat(throttle.stats().inUse()).isEqualTo(1); // outer unaffected
        }

        outer.close();
        assertThat(throttle.stats().inUse()).isZero();
        assertThat(throttle.stats().units()).isZero();
    }

    @Test
    void borrowsOnDifferentThreadsAreIndependentUnitsOfWork() throws Exception {
        // the root permit is tracked per thread, so a concurrent borrow elsewhere must not be mistaken for a child
        final Throttle throttle = throttle(2, config -> { });
        final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);

        asTenant("acme", () -> {
            try (Connection tenantConnection = dataSource.getConnection()) {
                assertThat(tenantConnection.isValid(1)).isTrue();
                onCleanThread(() -> {
                    try (Connection systemConnection = dataSource.getConnection()) {
                        assertThat(systemConnection.isValid(1)).isTrue();
                        assertThat(throttle.stats().inUse(null)).isEqualTo(1);
                        assertThat(throttle.stats().units()).isEqualTo(2); // two units, not one nested unit
                    }
                });
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
        assertThat(throttle.stats().inUse()).isZero();
    }

    @Test
    void tenantOverCapacityIsThrottledWithoutTouchingThePool() {
        final Throttle throttle = throttle(1, config -> { });
        try (final Permit permit = throttle.acquire("filler", Duration.ZERO)) { // capacity full, a different unit of work
            final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);

            asTenant("acme", () -> assertThatExceptionOfType(ThrottledException.class).isThrownBy(dataSource::getConnection));

            assertThat(activeConnections()).isZero(); // never reached the pool
        }
    }

    @Test
    void permitIsReleasedWhenTheUnderlyingPoolFails() {
        final Throttle throttle = throttle(2, config -> { });
        final ThrottlingDataSourceDecorator dataSource = dataSource(throttle);
        target.close(); // borrowing now fails after the permit has been taken

        asTenant("acme", () -> {
            assertThatExceptionOfType(SQLException.class).isThrownBy(dataSource::getConnection);
            assertThat(throttle.stats().inUse()).isZero(); // permit not leaked
            assertThat(throttle.stats().units()).isZero();
        });

        // the failed borrow also cleared the thread's root, so the next one starts a fresh unit of work
        assertThat(throttle.stats().units()).isZero();
    }

    /** Run on a fresh thread, so it has no tenant context and no root permit of its own. */
    private static void onCleanThread(final ThrowingRunnable body) throws Exception {
        final Exception[] failure = new Exception[1];
        final Thread thread = new Thread(() -> {
            try {
                body.run();
            } catch (final Exception e) {
                failure[0] = e;
            }
        });
        thread.start();
        thread.join();
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }

    private static Throttle throttle(final int capacity, final Consumer<ThrottleProperties> customizer) {
        final ThrottleProperties props = new ThrottleProperties();
        props.setThreshold(0); // always contended: fair share enforced from the first permit
        props.setSystemGranted(0); // priority off unless a test opts in
        customizer.accept(props);
        return new Throttle(props.toConfig(capacity));
    }

    private ThrottlingDataSourceDecorator dataSource(final Throttle throttle) {
        return new ThrottlingDataSourceDecorator(target, throttle, Duration.ZERO);
    }

    private int activeConnections() {
        return target.getHikariPoolMXBean().getActiveConnections();
    }
}
