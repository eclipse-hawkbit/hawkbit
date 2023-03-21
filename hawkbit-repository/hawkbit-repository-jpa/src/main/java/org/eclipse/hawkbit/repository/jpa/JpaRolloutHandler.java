/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link RolloutHandler}.
 */
@Validated
@Transactional(readOnly = true)
public class JpaRolloutHandler implements RolloutHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRolloutHandler.class);

    private final TenantAware tenantAware;
    private final RolloutManagement rolloutManagement;
    private final RolloutExecutor rolloutExecutor;
    private final LockRegistry lockRegistry;
    private final PlatformTransactionManager txManager;

    /**
     * Constructor
     * 
     * @param tenantAware
     *            the {@link TenantAware} bean holding the tenant information
     * @param rolloutManagement
     *            to fetch rollout related information from the datasource
     * @param rolloutExecutor
     *            to trigger executions for a specific rollout
     * @param lockRegistry
     *            to lock processes
     * @param txManager
     *            transaction manager interface
     */
    public JpaRolloutHandler(final TenantAware tenantAware, final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager) {
        this.tenantAware = tenantAware;
        this.rolloutManagement = rolloutManagement;
        this.rolloutExecutor = rolloutExecutor;
        this.lockRegistry = lockRegistry;
        this.txManager = txManager;
    }

    @Override
    // No transaction. Instead, a transaction will be created per handled rollout.
    @Transactional(propagation = Propagation.NEVER)
    public void handleAll() {
        final List<Rollout> rollouts = rolloutManagement.findActiveRollouts();

        if (rollouts.isEmpty()) {
            return;
        }

        final String tenant = tenantAware.getCurrentTenant();

        final String handlerId = createRolloutLockKey(tenant);
        final Lock lock = lockRegistry.obtain(handlerId);
        if (!lock.tryLock()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Could not perform lock {}", lock);
            }
            return;
        }

        try {
            LOGGER.trace("Trigger handling {} rollouts.", rollouts.size());
            rollouts.forEach(rolloutId -> DeploymentHelper.runInNewTransaction(txManager, handlerId + "-" + rolloutId,
                    status -> {
                        handleRollout(rolloutId);
                        return null;
                    }));
        } finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unlock lock {}", lock);
            }
            lock.unlock();
        }
    }

    public static String createRolloutLockKey(final String tenant) {
        return tenant + "-rollout";
    }

    private void handleRollout(final Rollout rollout) {
        runInUserContext(rollout, () -> rolloutExecutor.execute(rollout));
    }

    private void runInUserContext(final BaseEntity rollout, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler, () -> Objects.requireNonNull(rollout.getCreatedBy()),
                tenantAware);
    }

}
