/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to check target filters for auto assignment of distribution sets
 */
@Slf4j
public class AutoAssignScheduler {

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.scheduler.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;
    private final AutoAssignExecutor autoAssignExecutor;
    private final LockRegistry lockRegistry;
    private final Optional<MeterRegistry> meterRegistry;

    public AutoAssignScheduler(
            final SystemManagement systemManagement, final AutoAssignExecutor autoAssignExecutor,
            final LockRegistry lockRegistry, final Optional<MeterRegistry> meterRegistry) {
        this.systemManagement = systemManagement;
        this.autoAssignExecutor = autoAssignExecutor;
        this.lockRegistry = lockRegistry;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all tenants and runs for each
     * tenant the auto assignments defined in the target filter queries {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void autoAssignScheduler() {
        // run this code in system code privileged to have the necessary permission to query and create entities.
        SystemSecurityContext.runAsSystem(this::executeAutoAssign);
    }

    @SuppressWarnings("squid:S3516")
    private Object executeAutoAssign() {
        // workaround eclipselink that is currently not possible to execute a query without multitenancy if MultiTenant
        // annotation is used - https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So iterate through all tenants and execute the rollout
        // check for each tenant separately.
        final Lock lock = lockRegistry.obtain("scheduler");
        if (!lock.tryLock()) {
            return null;
        }

        final long startNano = System.nanoTime();
        try {
            log.debug("Auto assign scheduled execution has acquired lock and started for each tenant.");
            systemManagement.forEachTenant(tenant -> {
                final long startNanoT = System.nanoTime();

                autoAssignExecutor.checkAllTargets();

                meterRegistry
                        .map(mReg -> mReg.timer(
                                "hawkbit.scheduler.executor",
                                DefaultTenantConfiguration.TENANT_TAG, tenant))
                        .ifPresent(timer -> timer.record(System.nanoTime() - startNanoT, TimeUnit.NANOSECONDS));
            });
        } finally {
            lock.unlock();
            meterRegistry
                    .map(mReg -> mReg.timer("hawkbit.scheduler.executor.all"))
                    .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
            log.debug("Auto assign scheduled execution has released lock and finished.");
        }

        return null;
    }
}