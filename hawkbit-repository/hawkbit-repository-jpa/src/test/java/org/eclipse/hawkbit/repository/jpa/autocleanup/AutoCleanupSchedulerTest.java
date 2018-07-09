/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test class for {@link AutoCleanupScheduler}.
 *
 */
@Features("Component Tests - Repository")
@Stories("Auto cleanup scheduler")
public class AutoCleanupSchedulerTest extends AbstractJpaIntegrationTest {

    @Autowired
    private LockRegistry lockRegistry;

    @Test
    @Description("Verifies that all cleanup handlers are executed regardless if one of them throws an error")
    public void executeHandlerChain() {

        final AtomicInteger counter = new AtomicInteger();
        final CleanupTask successHandler = () -> {
            counter.incrementAndGet();
        };
        final CleanupTask failingHandler = () -> {
            counter.incrementAndGet();
            throw new RuntimeException("cleanup failed");
        };

        new AutoCleanupScheduler(systemManagement, systemSecurityContext, lockRegistry,
                Arrays.asList(successHandler, successHandler, failingHandler, successHandler)).run();

        assertThat(counter.get()).isEqualTo(4);

    }

}
