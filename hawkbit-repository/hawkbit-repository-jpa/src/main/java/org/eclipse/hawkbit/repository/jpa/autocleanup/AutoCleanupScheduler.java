/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * A scheduler to invoke a set of cleanup handlers periodically.
 */
public class AutoCleanupScheduler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoCleanupScheduler.class);

    private static final Object AUTO_CLEANUP = new Object();
    private static final String PROP_AUTO_CLEANUP_INTERVAL = "${hawkbit.autocleanup.scheduler.fixedDelay:86400000}";

    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final LockRegistry lockRegistry;
    private final List<Runnable> cleanupHandlers;

    /**
     * Constructs the cleanup schedulers and initializes it with a set of
     * cleanup handlers.
     * 
     * @param systemManagement
     *            Management APIs to invoke actions in a certain tenant context.
     * @param systemSecurityContext
     *            The system security context.
     * @param lockRegistry
     *            A registry for shared locks.
     * @param cleanupHandlers
     *            An array of cleanup handlers.
     */
    public AutoCleanupScheduler(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final LockRegistry lockRegistry,
            final Runnable... cleanupHandlers) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.lockRegistry = lockRegistry;
        this.cleanupHandlers = Arrays.asList(cleanupHandlers);
    }

    /**
     * Scheduler method which retrieves all tenants from
     * {@link SystemManagement#findTenants()} and runs all registered auto
     * cleanup handlers for each of them.
     */
    @Override
    @Scheduled(initialDelayString = PROP_AUTO_CLEANUP_INTERVAL, fixedDelayString = PROP_AUTO_CLEANUP_INTERVAL)
    public void run() {
        LOGGER.debug("Auto cleanup scheduler has been triggered.");
        // run this code in system code privileged to have the necessary
        // permission to query and create entities
        if (!cleanupHandlers.isEmpty()) {
            systemSecurityContext.runAsSystem(this::executeAutoCleanup);
        }
    }

    @SuppressWarnings("squid:S3516")
    private Void executeAutoCleanup() {
        final Lock lock = lockRegistry.obtain(AUTO_CLEANUP);
        if (!lock.tryLock()) {
            return null;
        }

        try {
            systemManagement.forEachTenant(tenant -> cleanupHandlers.forEach(handler -> {
                try {
                    handler.run();
                } catch (final RuntimeException e) {
                    LOGGER.error("One of the cleanup processes failed", e);
                }
            }));
        } finally {
            lock.unlock();
        }

        return null;
    }

}
