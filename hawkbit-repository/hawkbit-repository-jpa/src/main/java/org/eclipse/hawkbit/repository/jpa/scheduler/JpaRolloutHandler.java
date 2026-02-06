/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG;
import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG_VALUE_PROVIDER;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JPA implementation of {@link RolloutHandler}.
 */
@Slf4j
public class JpaRolloutHandler implements RolloutHandler {

    private final RolloutManagement rolloutManagement;
    private final RolloutExecutor rolloutExecutor;
    private final LockRegistry lockRegistry;
    private final PlatformTransactionManager txManager;
    private final Optional<MeterRegistry> meterRegistry;

    /**
     * Constructor
     *
     * @param rolloutManagement to fetch rollout related information from the datasource
     * @param rolloutExecutor to trigger executions for a specific rollout
     * @param lockRegistry to lock processes
     * @param txManager transaction manager interface
     */
    public JpaRolloutHandler(final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager, final Optional<MeterRegistry> meterRegistry) {
        this.rolloutManagement = rolloutManagement;
        this.rolloutExecutor = rolloutExecutor;
        this.lockRegistry = lockRegistry;
        this.txManager = txManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleAll() {
        final long startNano = System.nanoTime();

        final List<Long> rollouts = rolloutManagement.findActiveRollouts();
        if (rollouts.isEmpty()) {
            return;
        }

        final String handlerId = createRolloutLockKey(AccessContext.tenant());
        final Lock lock = lockRegistry.obtain(handlerId);
        if (!lock.tryLock()) {
            if (log.isTraceEnabled()) {
                log.trace("Could not obtain lock {}", lock);
            }
            return;
        }

        try {
            log.debug("Start handling {} rollouts.", rollouts.size());
            rollouts.forEach(rolloutId -> {
                try {
                    handleRolloutInNewTransaction(rolloutId, handlerId);
                } catch (final Throwable throwable) {
                    log.error("Failed to process rollout with id {}", rolloutId, throwable);
                }
            });
            log.debug("Finished handling of the rollouts.");
        } finally {
            lock.unlock();

            if (log.isTraceEnabled()) {
                log.trace("Unlock lock {}", lock);
            }
            meterRegistry // handle all rollouts of a tenant
                    .map(mReg -> mReg.timer("hawkbit.rollout.handle.all", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get()))
                    .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
        }
    }

    private static String createRolloutLockKey(final String tenant) {
        return tenant + "-rollout";
    }

    // run in a tenant context, i.e. Security.getCurrentTenant() returns the tenant the rollout is made for
    private void handleRolloutInNewTransaction(final long rolloutId, final String handlerId) {
        final long startNano = System.nanoTime();

        DeploymentHelper.runInNewTransaction(txManager, handlerId + "-" + rolloutId, status -> {
            rolloutManagement.find(rolloutId).ifPresentOrElse(
                    rolloutExecutor::execute,
                    () -> log.error("Could not retrieve rollout with id {}. Will not continue with execution.", rolloutId));
            return 0L;
        });

        meterRegistry // handle single rollout
                .map(mReg -> mReg.timer(
                        "hawkbit.rollout.handle", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get(), "rollout", String.valueOf(rolloutId)))
                .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
    }
}