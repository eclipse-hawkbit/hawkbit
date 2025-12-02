/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import org.eclipse.hawkbit.repository.jpa.rollout.BlockWhenFullPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class SchedulerUtils {

    private SchedulerUtils() {
    }

    /**
     * Creates a new {@link ThreadPoolTaskExecutor} with the given thread name
     * prefix and pool size.
     *
     * @param threadNamePrefix
     *            the prefix for the thread name
     * @param threadPoolSize
     *            the size of the pool
     * @return the new scheduler
     */
    public static ThreadPoolTaskExecutor threadPoolTaskExecutor(final String threadNamePrefix, final int threadPoolSize) {
        if (threadPoolSize <= 1) {
            return null;
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setQueueCapacity(0); // forces a Synchronous Queue
        // This policy will block the submitter until a worker thread is free
        executor.setRejectedExecutionHandler(new BlockWhenFullPolicy());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}

