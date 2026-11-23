/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;

/**
 * Test async executor (event multicaster, {@code @Async}) that counts the submitted but not yet finished tasks, so a
 * test can wait until all pending events are processed (see {@link #awaitIdle(Duration)}).
 * <p>
 * Events are dispatched asynchronously (as in production), so e.g. a cache evict event of an entity created in the test
 * setup could be processed in the middle of a measured section. Waiting for idle makes such tests deterministic without
 * switching the event dispatching to synchronous (which would run the listeners in the after commit phase of the
 * publishing transaction and differ from production).
 * <p>
 * It is an {@link ExecutorService} (the submit methods go through {@link #execute(Runnable)}, so they are tracked too)
 * since it overrides the auto-configured {@code asyncExecutor} which is referenced as such.
 */
public class PendingTasksTrackingExecutor extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final AtomicInteger pending = new AtomicInteger();

    public PendingTasksTrackingExecutor(final ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(final Runnable command) {
        pending.incrementAndGet();
        try {
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    // decremented after the run, so tasks (events) submitted by the task itself are already counted
                    pending.decrementAndGet();
                }
            });
        } catch (final RuntimeException e) {
            pending.decrementAndGet();
            throw e;
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    /** Waits until all submitted tasks, including the ones submitted meanwhile by the tasks themselves, are finished. */
    public void awaitIdle(final Duration timeout) {
        Awaitility.await().pollInSameThread().pollInterval(Duration.ofMillis(5)).atMost(timeout).until(() -> pending.get() == 0);
    }
}
