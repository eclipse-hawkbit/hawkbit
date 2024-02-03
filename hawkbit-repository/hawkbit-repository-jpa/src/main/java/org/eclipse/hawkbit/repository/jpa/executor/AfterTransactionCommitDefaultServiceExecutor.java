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
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A Service which calls register runnable. This runnables will executed after a
 * successful spring transaction commit.The class is thread safe.
 */
@Slf4j
public class AfterTransactionCommitDefaultServiceExecutor extends TransactionSynchronizationAdapter
        implements AfterTransactionCommitExecutor {
    
    private static final ThreadLocal<List<Runnable>> THREAD_LOCAL_RUNNABLES = new ThreadLocal<>();

    @Override
    // Exception squid:S1217 - Is aspectJ proxy
    @SuppressWarnings({ "squid:S1217" })
    public void afterCommit() {
        final List<Runnable> afterCommitRunnables = THREAD_LOCAL_RUNNABLES.get();
        log.debug("Transaction successfully committed, executing {} runnables", afterCommitRunnables.size());
        for (final Runnable afterCommitRunnable : afterCommitRunnables) {
            log.debug("Executing runnable {}", afterCommitRunnable);
            try {
                afterCommitRunnable.run();
            } catch (final RuntimeException e) {
                log.error("Failed to execute runnable " + afterCommitRunnable, e);
            }
        }
    }

    @Override
    // Exception squid:S1217 - we want to run this synchronous
    @SuppressWarnings("squid:S1217")
    public void afterCommit(final Runnable runnable) {
        log.debug("Submitting new runnable {} to run after transaction commit", runnable);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List<Runnable> localRunnables = THREAD_LOCAL_RUNNABLES.get();
            if (localRunnables == null) {
                localRunnables = new ArrayList<>();
                THREAD_LOCAL_RUNNABLES.set(localRunnables);
                TransactionSynchronizationManager.registerSynchronization(this);
            }
            localRunnables.add(runnable);
            return;
        }
        log.info("Transaction synchronization is NOT ACTIVE/ INACTIVE. Executing right now runnable {}", runnable);

        runnable.run();
    }

    @Override
    @SuppressWarnings({ "squid:S1217" })
    public void afterCompletion(final int status) {
        final String transactionStatus = status == STATUS_COMMITTED ? "COMMITTED" : "ROLLEDBACK";
        log.debug("Transaction completed after commit with status {}", transactionStatus);
        THREAD_LOCAL_RUNNABLES.remove();
    }

}
