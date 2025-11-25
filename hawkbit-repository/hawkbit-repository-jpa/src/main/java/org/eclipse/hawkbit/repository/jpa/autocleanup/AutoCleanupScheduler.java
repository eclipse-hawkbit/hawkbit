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

import java.util.List;
import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A scheduler to invoke a set of cleanup handlers periodically.
 */
@Slf4j
public class AutoCleanupScheduler {

    private static final String AUTO_CLEANUP = "auto-cleanup";
    private static final String SEP = ".";
    private static final String PROP_AUTO_CLEANUP_INTERVAL = "${hawkbit.autocleanup.scheduler.fixedDelay:86400000}";

    private final SystemManagement systemManagement;
    private final LockRegistry lockRegistry;
    private final List<CleanupTask> cleanupTasks;

    /**
     * Constructs the cleanup schedulers and initializes it with a set of cleanup handlers.
     *
     * @param cleanupTasks A list of cleanup tasks.
     * @param systemManagement Management APIs to invoke actions in a certain tenant context.
     * @param lockRegistry A registry for shared locks.
     */
    public AutoCleanupScheduler(
            final List<CleanupTask> cleanupTasks,
            final SystemManagement systemManagement, final LockRegistry lockRegistry) {
        this.systemManagement = systemManagement;
        this.lockRegistry = lockRegistry;
        this.cleanupTasks = cleanupTasks;
    }

    /**
     * Scheduler method which kicks off the cleanup process.
     */
    @Scheduled(initialDelayString = PROP_AUTO_CLEANUP_INTERVAL, fixedDelayString = PROP_AUTO_CLEANUP_INTERVAL)
    public void run() {
        log.debug("Auto cleanup scheduler has been triggered.");
        // run this code in system code privileged to have the necessary permission to query and create entities
        if (!cleanupTasks.isEmpty()) {
            System.asSystem(this::executeAutoCleanup);
        }
    }

    /**
     * Method which executes each registered cleanup task for each tenant.
     */
    @SuppressWarnings("squid:S3516")
    private Void executeAutoCleanup() {
        systemManagement.forEachTenant(tenant -> cleanupTasks.forEach(task -> {
            final Lock lock = lockRegistry.obtain(AUTO_CLEANUP + SEP + task.getId() + SEP + tenant);
            if (!lock.tryLock()) {
                return;
            }
            try {
                task.run();
            } catch (final RuntimeException e) {
                log.error("Cleanup task failed.", e);
            } finally {
                lock.unlock();
            }
        }));
        return null;
    }

    /**
     * Interface modeling a cleanup task.
     */
    public interface CleanupTask extends Runnable {

        /**
         * Executes the cleanup task.
         */
        @Override
        void run();

        /**
         * @return The identifier of this cleanup task. Never null.
         */
        String getId();
    }
}