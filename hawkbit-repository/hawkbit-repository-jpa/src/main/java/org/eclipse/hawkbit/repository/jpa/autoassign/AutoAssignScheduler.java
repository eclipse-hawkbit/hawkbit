/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to check target filters for auto assignment of distribution sets
 */
public class AutoAssignScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignScheduler.class);

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.autoassign.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;

    private final SystemSecurityContext systemSecurityContext;

    private final AutoAssignChecker autoAssignChecker;

    private final LockRegistry lockRegistry;

    /**
     * Instantiates a new AutoAssignScheduler
     * 
     * @param systemManagement
     *            to find all tenants
     * @param systemSecurityContext
     *            to run as system
     * @param autoAssignChecker
     *            to run a check as tenant
     * @param lockRegistry
     *            to acquire a lock per tenant
     */
    public AutoAssignScheduler(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final AutoAssignChecker autoAssignChecker,
            final LockRegistry lockRegistry) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.autoAssignChecker = autoAssignChecker;
        this.lockRegistry = lockRegistry;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the auto assignments defined in the target filter queries
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void autoAssignScheduler() {
        LOGGER.debug("auto assign schedule checker has been triggered.");
        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> executeAutoAssign());
    }

    private Object executeAutoAssign() {
        // workaround eclipselink that is currently not possible to
        // execute a query without multitenancy if MultiTenant
        // annotation is used.
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
        // iterate through all tenants and execute the rollout check for
        // each tenant separately.
        final Lock lock = lockRegistry.obtain("autoassign");
        if (!lock.tryLock()) {
            return null;
        }

        try {
            systemManagement.forEachTenant(tenant -> autoAssignChecker.check());
        } finally {
            lock.unlock();
        }

        return null;
    }
}
