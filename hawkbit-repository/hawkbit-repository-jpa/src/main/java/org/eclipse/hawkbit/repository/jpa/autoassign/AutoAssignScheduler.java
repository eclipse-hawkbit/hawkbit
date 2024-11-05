/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to check target filters for auto assignment of distribution sets
 */
@Slf4j
public class AutoAssignScheduler {

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.autoassign.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;

    private final SystemSecurityContext systemSecurityContext;

    private final AutoAssignExecutor autoAssignExecutor;

    private final LockRegistry lockRegistry;

    /**
     * Instantiates a new AutoAssignScheduler
     *
     * @param systemManagement to find all tenants
     * @param systemSecurityContext to run as system
     * @param autoAssignExecutor to run a check as tenant
     * @param lockRegistry to acquire a lock per tenant
     */
    public AutoAssignScheduler(final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final AutoAssignExecutor autoAssignExecutor,
            final LockRegistry lockRegistry) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.autoAssignExecutor = autoAssignExecutor;
        this.lockRegistry = lockRegistry;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants and runs for each
     * tenant the auto assignments defined in the target filter queries
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void autoAssignScheduler() {
        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(this::executeAutoAssign);
    }

    @SuppressWarnings("squid:S3516")
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
            log.debug("Auto assign scheduled execution has acquired lock and started for each tenant.");
            systemManagement.forEachTenant(tenant -> autoAssignExecutor.checkAllTargets());
        } finally {
            lock.unlock();
            log.debug("Auto assign scheduled execution has released lock and finished.");
        }

        return null;
    }
}
