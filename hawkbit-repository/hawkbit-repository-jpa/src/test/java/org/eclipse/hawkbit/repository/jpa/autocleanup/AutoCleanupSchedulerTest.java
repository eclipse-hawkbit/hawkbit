/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.autocleanup.AutoCleanupScheduler.CleanupTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Test class for {@link AutoCleanupScheduler}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Auto cleanup scheduler
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
class AutoCleanupSchedulerTest extends AbstractJpaIntegrationTest {

    private final AtomicInteger counter = new AtomicInteger();

    @Autowired
    private LockRegistry lockRegistry;

    @BeforeEach
    void setUp() {
        counter.set(0);
    }

    /**
     * Verifies that all cleanup handlers are executed regardless if one of them throws an error
     */
    @Test
    void executeHandlerChain() {
        new AutoCleanupScheduler(
                List.of(new SuccessfulCleanup(), new SuccessfulCleanup(), new FailingCleanup(), new SuccessfulCleanup()),
                systemManagement, lockRegistry).run();
        assertThat(counter.get()).isEqualTo(4);
    }

    private class SuccessfulCleanup implements CleanupTask {

        @Override
        public void run() {
            counter.incrementAndGet();
        }

        @Override
        public String getId() {
            return "success";
        }

    }

    private class FailingCleanup implements CleanupTask {

        @Override
        public void run() {
            counter.incrementAndGet();
            throw new RuntimeException("cleanup failed");
        }

        @Override
        public String getId() {
            return "success";
        }

    }

}
