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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Scheduler to schedule the {@link RolloutHandler#handleAll()}. The
 * delay between the checks be configured using the property from
 * {#PROP_SCHEDULER_DELAY_PLACEHOLDER}.
 */
@Slf4j
public class RolloutScheduler {

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;
    private final RolloutHandler rolloutHandler;
    private final Optional<MeterRegistry> meterRegistry;
    private final ThreadPoolTaskExecutor rolloutTaskExecutor;

    public RolloutScheduler(
            final RolloutHandler rolloutHandler, final SystemManagement systemManagement,
            final int threadPoolSize, final Optional<MeterRegistry> meterRegistry) {
        this.systemManagement = systemManagement;
        this.rolloutHandler = rolloutHandler;
        this.meterRegistry = meterRegistry;
        rolloutTaskExecutor = SchedulerUtils.threadPoolTaskExecutor("rollout-exec-", threadPoolSize);

    }

    /**
     * Scheduler method called by the spring-async mechanism. For all tenants, using {@link SystemManagement#forEachTenantAsSystem},
     * runs the {@link RolloutHandler#handleAll()} scoped to permission of access control context or unscoped (with {@link System}) if null
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void runningRolloutScheduler() {
        log.debug("rollout schedule checker has been triggered.");
        final long startNano = java.lang.System.nanoTime();

        // run this code in system code privileged to have the necessary system code permission execute forEachTenant
        asSystem(() ->
                // workaround eclipselink that is currently not possible to execute a query without multi-tenancy if MultiTenant
                // annotation is used. https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
                // iterate through all tenants and execute the rollout check for each tenant separately.
                systemManagement.forEachTenantAsSystem(tenant -> {
                    if (rolloutTaskExecutor == null) {
                        handleAll(tenant);
                    } else {
                        rolloutTaskExecutor.submit(() -> asSystemAsTenant(tenant, () -> handleAll(tenant)));
                    }
                }));

        meterRegistry
                .map(mReg -> mReg.timer("hawkbit.rollout.scheduler.all"))
                .ifPresent(timer -> timer.record(java.lang.System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
    }

    private void handleAll(final String tenant) {
        log.trace("Handling rollout for tenant: {}", tenant);
        final long startNano = java.lang.System.nanoTime();

        try {
            rolloutHandler.handleAll();
        } catch (final Exception e) {
            log.error("Error processing rollout for tenant {}", tenant, e);
        }

        meterRegistry
                .map(mReg -> mReg.timer("hawkbit.rollout.scheduler", DefaultTenantConfiguration.TENANT_TAG, tenant))
                .ifPresent(timer -> timer.record(java.lang.System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
    }

}