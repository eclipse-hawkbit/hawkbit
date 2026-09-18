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

import java.time.Duration;
import java.util.function.Consumer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.hawkbit.throttle.Throttle;
import org.eclipse.hawkbit.throttle.ThrottleProperties.ThrottleConfig;
import org.eclipse.hawkbit.throttle.ThrottledException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test for {@code @Transactional(propagation = Propagation.REQUIRES_NEW)} against a real HikariCP pool.
 *
 * <p>A nested transaction takes a <em>child</em> permit of the outer one, so permits stay a one-to-one mirror of
 * borrowed connections while both belong to a single unit of work. The child is admitted ahead of the fair share,
 * because a caller blocked while holding a connection is what deadlocks the pool; and when even that cannot be
 * satisfied it is refused rather than parked.
 */
@EnableTransactionManagement
class ThrottlingRequiresNewTransactionTest {

    private HikariDataSource rawDataSource;

    @BeforeEach
    void setUp() {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:requires_new_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setMaximumPoolSize(10);
        rawDataSource = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        rawDataSource.close();
    }

    @Test
    void requiresNewTakesAChildPermitSoPermitsMatchConnections() {
        final Throttle throttle = throttle(2, config -> { });
        final PlatformTransactionManager txManager = txManager(throttle);

        asTenant("acme", () -> outer(txManager).executeWithoutResult(status -> {
            assertThat(throttle.stats().inUse("acme")).isEqualTo(1);
            assertThat(activeConnections()).isEqualTo(1);

            requiresNew(txManager).executeWithoutResult(innerStatus -> {
                // two real connections AND two permits — the throttle no longer under-counts the pool
                assertThat(activeConnections()).isEqualTo(2);
                assertThat(throttle.stats().inUse("acme")).isEqualTo(2);
                assertThat(throttle.stats().units()).isEqualTo(1); // still one unit of work
            });

            // inner closed independently of the outer
            assertThat(activeConnections()).isEqualTo(1);
            assertThat(throttle.stats().inUse("acme")).isEqualTo(1);
        }));

        assertThat(throttle.stats().inUse()).isZero();
        assertThat(throttle.stats().units()).isZero();
    }

    @Test
    void requiresNewBypassesFairShareWhenTenantAtItsLimit() {
        // ceiling of 1 for every tenant: a fresh unit of work would be refused, the in-flight one's child must not be
        final Throttle throttle = throttle(2, config -> config.setLimit(1));
        final PlatformTransactionManager txManager = txManager(throttle);

        asTenant("acme", () -> outer(txManager).executeWithoutResult(status -> {
            assertThat(throttle.stats().inUse("acme")).isEqualTo(1); // at its ceiling

            requiresNew(txManager).executeWithoutResult(
                    innerStatus -> assertThat(throttle.stats().inUse("acme")).isEqualTo(2));
        }));
    }

    @Test
    void requiresNewFailsFastInsteadOfDeadlockingWhenCapacityIsExhausted() {
        // capacity 1: the outer transaction is the sole unit of work, so parking its child would wedge the pool
        final Throttle throttle = throttle(1, config -> { });
        final PlatformTransactionManager txManager = txManager(throttle);

        asTenant("acme", () -> outer(txManager).executeWithoutResult(status -> {
            assertThat(throttle.stats().inUse("acme")).isEqualTo(1);

            // Spring wraps the refusal; what matters is that it returns instead of parking on a wedged pool
            assertThatExceptionOfType(CannotCreateTransactionException.class)
                    .isThrownBy(() -> requiresNew(txManager).executeWithoutResult(innerStatus -> { }))
                    .havingRootCause()
                    .isInstanceOf(ThrottledException.class)
                    .withMessageContaining("deadlock");
        }));

        assertThat(throttle.stats().inUse()).isZero(); // outer rolled back and released
    }

    private int activeConnections() {
        return rawDataSource.getHikariPoolMXBean().getActiveConnections();
    }

    private static TransactionTemplate outer(final PlatformTransactionManager txManager) {
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(Propagation.REQUIRED.value());
        return template;
    }

    private static TransactionTemplate requiresNew(final PlatformTransactionManager txManager) {
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        return template;
    }

    private static Throttle throttle(final int capacity, final Consumer<ThrottleConfig> customizer) {
        final ThrottleConfig config = new ThrottleConfig();
        config.setThreshold(0);
        config.setSystemFloorPercent(0);
        customizer.accept(config);
        return new Throttle(config.toPolicy(capacity));
    }

    private PlatformTransactionManager txManager(final Throttle throttle) {
        return new DataSourceTransactionManager(new ThrottlingDataSourceDecorator(rawDataSource, throttle, Duration.ofSeconds(1)));
    }
}
