/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.executor;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A Service which calls register runnable. This runnables will be executed after a successful spring transaction commit.
 * The class is thread safe.
 */
@Slf4j
public class AfterTransactionCommitDefaultServiceExecutor implements AfterTransactionCommitExecutor {

    private static class TransactionSynchronizationImpl implements TransactionSynchronization {

        private final List<Runnable> afterCommitRunnables = new ArrayList<>();

        @Override
        // Exception squid:S1217 - Is aspectJ proxy
        @SuppressWarnings({ "squid:S1217" })
        public void afterCommit() {
            log.debug("Transaction successfully committed, executing {} runnables", afterCommitRunnables.size());
            for (final Runnable afterCommitRunnable : afterCommitRunnables) {
                log.debug("Executing runnable {}", afterCommitRunnable);
                try {
                    afterCommitRunnable.run();
                } catch (final RuntimeException e) {
                    log.error("Failed to execute runnable {}", afterCommitRunnable, e);
                }
            }
        }

        @Override
        @SuppressWarnings({ "squid:S1217" })
        public void afterCompletion(final int status) {
            log.debug("Transaction completed after commit with status {}", status == STATUS_COMMITTED ? "COMMITTED" : "ROLLEDBACK");
        }

        private void afterCommit(final Runnable runnable) {
            afterCommitRunnables.add(runnable);
        }
    }

    @Override
    // Exception squid:S1217 - we want to run this synchronous
    @SuppressWarnings("squid:S1217")
    public void afterCommit(final Runnable runnable) {
        log.debug("Submitting new runnable {} to run after transaction commit", runnable);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.getSynchronizations().stream().filter(TransactionSynchronizationImpl.class::isInstance)
                    .map(TransactionSynchronizationImpl.class::cast).findAny().orElseGet(() -> {
                        final TransactionSynchronizationImpl newTS = new TransactionSynchronizationImpl();
                        TransactionSynchronizationManager.registerSynchronization(newTS);
                        return newTS;
                    }).afterCommit(runnable);
        } else {
            log.info("Transaction synchronization is NOT ACTIVE/ INACTIVE. Executing right now runnable {}", runnable);
            runnable.run();
        }
    }
}