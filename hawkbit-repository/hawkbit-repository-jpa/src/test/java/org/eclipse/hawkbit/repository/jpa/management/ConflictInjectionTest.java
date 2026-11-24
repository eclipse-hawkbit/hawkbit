/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.test.util.ConflictInjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Self-test of the {@link ConflictInjection} helper used by the rollout concurrency tests.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Conflict injection
 */
class ConflictInjectionTest extends AbstractJpaIntegrationTest {

    private static final String CHANGED_ADDRESS = "http://device.example/changed";

    @Autowired
    private PlatformTransactionManager txManager;

    /**
     * The callback runs in its own committed transaction right after the intercepted read: the read returns the old
     * state, a re-read shows the callback's change with a higher optimistic lock revision.
     */
    @Test
    void callbackChangeIsCommittedAfterInterceptedRead() {
        final long targetId = testdataFactory.createTarget("conflict-self-test").getId();

        final JpaTarget readBefore;
        try (ConflictInjection ignored = ConflictInjection.afterRead(txManager, targetRepository, "findById", () -> {
            final JpaTarget target = targetRepository.findById(targetId).orElseThrow();
            target.setAddress(CHANGED_ADDRESS);
            targetRepository.save(target);
        })) {
            readBefore = targetRepository.findById(targetId).orElseThrow();
        }
        final JpaTarget reRead = targetRepository.findById(targetId).orElseThrow();

        assertThat(readBefore.getAddress()).isNotEqualTo(CHANGED_ADDRESS);
        assertThat(reRead.getAddress()).isEqualTo(CHANGED_ADDRESS);
        assertThat(reRead.getOptLockRevision()).isGreaterThan(readBefore.getOptLockRevision());
    }
}
