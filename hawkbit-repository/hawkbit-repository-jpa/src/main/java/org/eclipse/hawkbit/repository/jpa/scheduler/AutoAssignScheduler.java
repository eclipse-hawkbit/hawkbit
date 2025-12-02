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

import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.context.AccessContext.asSystemAsTenant;
import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Scheduler to check target filters for auto assignment of distribution sets
 */
@Slf4j
public class AutoAssignScheduler {

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.autoassign.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;
    private final AutoAssignExecutor autoAssignExecutor;
    private final LockRegistry lockRegistry;
    private final Optional<MeterRegistry> meterRegistry;
    private final ThreadPoolTaskExecutor autoAssignTaskExecutor;

    public AutoAssignScheduler(
            final SystemManagement systemManagement, final AutoAssignExecutor autoAssignExecutor,
            final int threadPoolSize,
            final LockRegistry lockRegistry, final Optional<MeterRegistry> meterRegistry) {
        this.systemManagement = systemManagement;
        this.autoAssignExecutor = autoAssignExecutor;
        this.lockRegistry = lockRegistry;
        this.meterRegistry = meterRegistry;
        autoAssignTaskExecutor = SchedulerUtils.threadPoolTaskExecutor("auto-assign-exec-", threadPoolSize);
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all tenants and runs for each
     * tenant the auto assignments defined in the target filter queries {@link System}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void autoAssignScheduler() {
        // run this code in system code privileged to have the necessary permission to query and create entities.
        log.debug("Triggered auto-assign scheduler.");
        final long startNano = System.nanoTime();

        asSystem(() ->
                systemManagement.forEachTenantAsSystem(tenant -> {
                    if (autoAssignTaskExecutor == null) {// sync
                        handleAll(tenant);
                    } else {// async
                        autoAssignTaskExecutor.execute(() -> asSystemAsTenant(tenant, () -> handleAll(tenant)));
                    }
                })
        );

        meterRegistry // handle all tenants (some could be skipped if lock could not be obtained)
                .map(mReg -> mReg.timer("hawkbit.autoassign.scheduler"))
                .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
        log.debug("Finished auto-assign scheduler run.");
    }

    private void handleAll(final String tenant) {
        final Lock lock = lockRegistry.obtain(createAutoAssignmentLockKey(tenant));
        if (!lock.tryLock()) {
            return;
        }

        final long startNano = System.nanoTime();
        try {
            autoAssignExecutor.checkAllTargets();
        } finally {
            lock.unlock();

            meterRegistry
                    .map(mReg -> mReg.timer("hawkbit.autoassign.handle.all", TENANT_TAG, tenant))
                    .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
        }
    }

    private static String createAutoAssignmentLockKey(final String tenant) {
        return tenant + "-auto-assign";
    }
}