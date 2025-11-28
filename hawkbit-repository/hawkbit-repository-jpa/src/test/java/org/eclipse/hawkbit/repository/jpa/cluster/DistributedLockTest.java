/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: Distributed Lock
 */
@SpringBootTest(classes = { DistributedLockTest.Config.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Slf4j
class DistributedLockTest extends AbstractJpaIntegrationTest {

    @Autowired
    private LockProperties lockProperties;

    @Autowired
    @Qualifier("lockRepository0")
    private LockRepository lockRepository0;

    @Autowired
    @Qualifier("lockRepository1")
    private LockRepository lockRepository1;

    @EnableTransactionManagement
    @Configuration
    @EnableConfigurationProperties({ LockProperties.class })
    @PropertySource("classpath:/jpa-test.properties")
    static class Config {

        @Bean
        @ConditionalOnMissingBean
        public UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager();
        }

        @Bean
        LockProperties lockProperties() {
            return new LockProperties();
        }

        @Bean
        LockRepository lockRepository0(final DataSource dataSource, final LockProperties lockProperties,
                final PlatformTransactionManager txManager) {
            return lockRepository(dataSource, lockProperties, txManager);
        }

        @Bean
        LockRepository lockRepository1(final DataSource dataSource, final LockProperties lockProperties,
                final PlatformTransactionManager txManager) {
            return lockRepository(dataSource, lockProperties, txManager);
        }

        private LockRepository lockRepository(final DataSource dataSource, final LockProperties lockProperties,
                final PlatformTransactionManager txManager) {
            final DefaultLockRepository repository = new DistributedLockRepository(dataSource, lockProperties, txManager);
            repository.setPrefix("SP_");
            return repository;
        }
    }

    /**
     * Test to verify that lock is kept while ping runs
     */
    @SuppressWarnings({ "java:S2925" })
    @Test
    void keepLockAlive() {
        final LockRegistry lockRegistry0 = new JdbcLockRegistry(lockRepository0);
        final LockRegistry lockRegistry1 = new JdbcLockRegistry(lockRepository1);

        final String lockKey0 = "test-lock0";
        final String lockKey1 = "test-lock1";
        // JDBCLockRegistry#pathFor
        final String path0 = UUIDConverter.getUUID(lockKey0).toString();
        final String path1 = UUIDConverter.getUUID(lockKey1).toString();
        // lock{i}{j} -> lockKey{i} obtained by lockRegistry{j}
        final Lock lock00 = lockRegistry0.obtain(lockKey0);
        final Lock lock01 = lockRegistry1.obtain(lockKey0);
        final Lock lock10 = lockRegistry0.obtain(lockKey1);
        final Lock lock11 = lockRegistry1.obtain(lockKey1);

        final AtomicBoolean lock01Obtained = new AtomicBoolean();
        final AtomicBoolean lock11Obtained = new AtomicBoolean();

        final AtomicBoolean lock11Locked = new AtomicBoolean(); // state of the lock11
        log.info("Starting test");
        // service 0 must be able to lock lockKey0
        assertThat(lock00.tryLock()).isTrue();
        try {
            assertThat(lockRepository0.isAcquired(path0)).isTrue(); // check db state

            final Thread lockThread1 = new Thread(() -> {
                // asserts lockKey1 is free and could be locked
                assertThat(lock11.tryLock()).isTrue();
                assertThat(lockRepository1.isAcquired(path1)).isTrue(); // check db state

                try {
                    lock11Obtained.set(true);
                    lock11Locked.set(true);

                    // asserts lockKey0 is kept by lock00 and could not be locked via lockRepository1
                    try {
                        final Instant timeout = Instant.now().plus(4 * lockProperties.getTtl(), ChronoUnit.MILLIS);
                        while (Instant.now().isBefore(timeout)) {
                            log.debug("lockThread1: loop, timeout: {}", new Date(timeout.toEpochMilli()));
                            assertThat(lock01.tryLock()).isFalse();
                            assertThat(lockRepository1.isAcquired(path0)).isFalse(); // check db state

                            waitMillis(Math.min(1, lockProperties.getTtl() / 4));
                        }
                    } catch (final AssertionError e) {
                        log.error("lockRepository1 has locked lockKey0 which has to be in lockRepository0 possession!", e);
                        lock01Obtained.set(true);
                        lock01.unlock();
                    }

                    assertThat(lockRepository0.isAcquired(path1)).isFalse(); // check db state
                    assertThat(lockRepository1.isAcquired(path1)).isTrue(); // check db state
                } finally {
                    lock11Locked.set(false);
                    lock11.unlock();
                    assertThat(lockRepository1.isAcquired(path1)).isFalse(); // check db state
                }
            });
            lockThread1.start();

            // asserts lockKey1 is kept by lock11 and could not be locked via lockRepository0
            final Instant timeout = Instant.now().plus(4 * lockProperties.getTtl(), ChronoUnit.MILLIS);
            while (Instant.now().isBefore(timeout)) {
                log.debug("main thread: loop, timeout: {}", new Date(timeout.toEpochMilli()));
                if (lock11Locked.get()) {
                    try {
                        assertThat(lock10.tryLock()).isFalse();
                        assertThat(lockRepository0.isAcquired(path1)).isFalse(); // check db state
                    } catch (final AssertionError e) {
                        log.error("lockRepository0 has locked lockKey1 which has to be in lockRepository1 possession!");
                        lock10.unlock();
                        if (lock11Locked.get()) {
                            throw e;
                        } else {
                            // otherwise the lock has been released
                            break;
                        }
                    }

                    waitMillis(Math.min(1, lockProperties.getTtl() / 4));
                }
            }

            try {
                lockThread1.join();
            } catch (final InterruptedException e) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                }
            }

            // assert that service 1 hasn't been able to acquire the lock 0
            assertThat(lock01Obtained).isFalse();
            // assert that service 1 has been able to acquire the lock 1
            assertThat(lock11Obtained).isTrue();

            assertThat(lockRepository0.isAcquired(path0)).isTrue(); // check db state
            assertThat(lockRepository1.isAcquired(path0)).isFalse(); // check db state
        } finally {
            lock00.unlock();
            assertThat(lockRepository0.isAcquired(path0)).isFalse(); // check db state
        }

        try {
            // assert that lockKey1 has been released by lock11 and could be got again 
            // and in different thread
            assertThat(lock10.tryLock()).isTrue();
            // and can't be locked by it while locked by lock01
            assertThat(lock11.tryLock()).isFalse();
        } finally {
            lock10.unlock();
        }

        // assert that db is clean
        assertThat(lockRepository0.isAcquired(path0)).isFalse();
        assertThat(lockRepository1.isAcquired(path0)).isFalse();
        assertThat(lockRepository0.isAcquired(path1)).isFalse();
        assertThat(lockRepository1.isAcquired(path1)).isFalse();
    }
}
