/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Waiting for a physical JDBC connection (exhausted pool, connection throttle) against the real persistence stack - the transaction begin
 * of other threads must not be stalled meanwhile (EclipseLinkJpaDialect holds a dialect wide lock while EclipseLink acquires the
 * connection, see HawkbitEclipseLinkJpaDialect#beginTransaction). A refused connection still fails the transaction begin, before any
 * transactional code runs.
 */
@Import(TransactionBeginConnectionWaitTest.GateConfiguration.class)
class TransactionBeginConnectionWaitTest extends AbstractJpaIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    // own threads - not the common fork join pool, which may be busy with other work in the same JVM
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager txManager;

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    /**
     * The waiting transaction is a custom isolation one or a default isolation write one - the two transaction begins that acquire the
     * connection under the EclipseLinkJpaDialect lock. Meanwhile, transactions of other threads (custom isolation, default isolation write
     * and read only) begin, run a statement and commit.
     */
    @ParameterizedTest
    @ValueSource(ints = { TransactionDefinition.ISOLATION_READ_COMMITTED, TransactionDefinition.ISOLATION_DEFAULT })
    void waitingForAConnectionDoesNotBlockTheTransactionBeginOfOtherThreads(final int waitingIsolation) throws InterruptedException {
        final Gate gate = Gate.park();
        final CompletableFuture<Void> waiting = CompletableFuture.runAsync(
                () -> Gate.gated(() -> query(waitingIsolation, false)), executor);
        assertThat(gate.parked.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();

        try {
            assertThat(CompletableFuture.runAsync(() -> query(TransactionDefinition.ISOLATION_READ_COMMITTED, false), executor))
                    .succeedsWithin(TIMEOUT);
            assertThat(CompletableFuture.runAsync(() -> query(TransactionDefinition.ISOLATION_DEFAULT, false), executor))
                    .succeedsWithin(TIMEOUT);
            assertThat(CompletableFuture.runAsync(() -> query(TransactionDefinition.ISOLATION_DEFAULT, true), executor))
                    .succeedsWithin(TIMEOUT);
            assertThat(waiting).isNotDone();
        } finally {
            gate.release.countDown();
        }
        assertThat(waiting).succeedsWithin(TIMEOUT);
    }

    @Test
    void refusedConnectionFailsTheTransactionBegin() {
        final IllegalStateException refused = new IllegalStateException("refused");
        Gate.refuse(refused);
        final AtomicBoolean executed = new AtomicBoolean();

        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThatExceptionOfType(CannotCreateTransactionException.class)
                .isThrownBy(() -> Gate.gated(() -> template.executeWithoutResult(status -> executed.set(true))))
                .havingRootCause()
                .isSameAs(refused);
        assertThat(executed).isFalse();
    }

    private void query(final int isolationLevel, final boolean readOnly) {
        final TransactionTemplate template = new TransactionTemplate(txManager);
        template.setIsolationLevel(isolationLevel);
        template.setReadOnly(readOnly);
        template.executeWithoutResult(status -> entityManager.createNativeQuery("SELECT 1").getSingleResult());
    }

    // gates the connection acquisition of the gated thread - parks it until released or refuses it
    private static final class Gate {

        private static final ThreadLocal<Boolean> GATED = new ThreadLocal<>();
        private static volatile Gate current;

        private final CountDownLatch parked = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final RuntimeException refusal;

        private Gate(final RuntimeException refusal) {
            this.refusal = refusal;
        }

        private static Gate park() {
            current = new Gate(null);
            return current;
        }

        private static void refuse(final RuntimeException refusal) {
            current = new Gate(refusal);
        }

        private static void gated(final Runnable runnable) {
            GATED.set(true);
            try {
                runnable.run();
            } finally {
                GATED.remove();
            }
        }

        private static void pass() {
            final Gate gate = current;
            if (gate == null || GATED.get() == null) {
                return;
            }
            if (gate.refusal != null) {
                throw gate.refusal;
            }
            gate.parked.countDown();
            try {
                gate.release.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Configuration
    static class GateConfiguration {

        @Bean
        static BeanPostProcessor gateDataSourcePostProcessor() {
            return new BeanPostProcessor() {

                @Override
                public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                    if (bean instanceof DataSource dataSource && !(bean instanceof DelegatingDataSource)) {
                        return new DelegatingDataSource(dataSource) {

                            @Override
                            public Connection getConnection() throws SQLException {
                                Gate.pass();
                                return super.getConnection();
                            }
                        };
                    }
                    return bean;
                }
            };
        }
    }
}
