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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies the master switch actually gates the DataSource wrapping: enabled ⇒ the application
 * DataSource is replaced by a {@link ThrottlingDataSourceDecorator}; disabled ⇒ the DataSource is
 * left untouched (byte-for-byte current behavior).
 */
class ThrottleAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ThrottleAutoConfiguration.class))
            .withBean("dataSource", DataSource.class, ThrottleAutoConfigurationTest::h2DataSource);

    @Test
    void wrapsDataSourceWhenEnabled() {
        runner.withPropertyValues("hawkbit.throttle.enabled=true")
                .run(context -> assertThat(context.getBean(DataSource.class)).isInstanceOf(ThrottlingDataSourceDecorator.class));
    }

    @Test
    void leavesDataSourceUntouchedWhenDisabled() {
        // enabled defaults to false → auto-configuration skipped entirely
        runner.run(context -> assertThat(context.getBean(DataSource.class)).isNotInstanceOf(ThrottlingDataSourceDecorator.class));
    }

    private static DataSource h2DataSource() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:throttleac_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(4);
        return new HikariDataSource(config);
    }
}
