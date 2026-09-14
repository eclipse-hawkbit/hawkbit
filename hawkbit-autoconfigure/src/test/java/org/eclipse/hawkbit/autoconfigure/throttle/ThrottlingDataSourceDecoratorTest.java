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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.function.ToIntFunction;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.hawkbit.throttle.TenantThrottle;
import org.eclipse.hawkbit.throttle.ThrottledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end Layer-2 verification: the decorator + {@code ConnectionThrottleGate} + engine against a
 * real HikariCP pool over H2. Proves the whole DataSource path, not just the unit-tested logic.
 */
class ThrottlingDataSourceDecoratorTest {

    private static final ToIntFunction<String> UNLIMITED = tenant -> -1;

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
        final TenantThrottle throttle = new TenantThrottle(5, 0, UNLIMITED);
        final ThrottlingDataSourceDecorator dataSource = new ThrottlingDataSourceDecorator(target, throttle, Duration.ofSeconds(1));

        asTenant("acme", () -> {
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(1); // real query ran through the proxied connection
                assertThat(throttle.inUse("acme")).isEqualTo(1);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

            assertThat(throttle.inUse("acme")).isZero(); // permit released when the connection returned to the pool
        });
    }

    @Test
    void backgroundThreadWithoutMarkerBypassesThrottle() throws Exception {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        throttle.acquire("filler", Duration.ZERO); // engine capacity consumed by another holder
        final ThrottlingDataSourceDecorator dataSource = new ThrottlingDataSourceDecorator(target, throttle, Duration.ZERO);

        // no marker on this thread → exempt, must still get a real connection despite the full engine
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
        assertThat(throttle.inUse()).isEqualTo(1); // unchanged: only the filler holds a permit
    }

    @Test
    void tenantOverCapacityIsThrottled() {
        final TenantThrottle throttle = new TenantThrottle(1, 0, UNLIMITED);
        throttle.acquire("filler", Duration.ZERO); // capacity full
        final ThrottlingDataSourceDecorator dataSource = new ThrottlingDataSourceDecorator(target, throttle, Duration.ZERO);

        asTenant("acme", () -> assertThatExceptionOfType(ThrottledException.class).isThrownBy(dataSource::getConnection));
    }
}
