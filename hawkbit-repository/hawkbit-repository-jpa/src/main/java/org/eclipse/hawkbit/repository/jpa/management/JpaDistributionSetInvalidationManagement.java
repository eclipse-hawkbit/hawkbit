/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Jpa implementation for {@link DistributionSetInvalidationManagement}
 */
@Slf4j
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-invalidation-management" }, matchIfMissing = true)
public class JpaDistributionSetInvalidationManagement implements DistributionSetInvalidationManagement {

    private final DistributionSetManagement<? extends DistributionSet> distributionSetManagement;
    private final RolloutManagement rolloutManagement;
    private final DeploymentManagement deploymentManagement;
    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final PlatformTransactionManager txManager;
    private final RepositoryProperties repositoryProperties;
    private final LockRegistry lockRegistry;

    @SuppressWarnings("java:S107")
    protected JpaDistributionSetInvalidationManagement(
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement,
            final RolloutManagement rolloutManagement, final DeploymentManagement deploymentManagement,
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final PlatformTransactionManager txManager, final RepositoryProperties repositoryProperties,
            final LockRegistry lockRegistry) {
        this.distributionSetManagement = distributionSetManagement;
        this.rolloutManagement = rolloutManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.txManager = txManager;
        this.repositoryProperties = repositoryProperties;
        this.lockRegistry = lockRegistry;
    }

    @Override
    public void invalidateDistributionSet(final DistributionSetInvalidation distributionSetInvalidation) {
        log.debug("Invalidate distribution sets {}", distributionSetInvalidation.getDistributionSetIds());
        final String tenant = AccessContext.tenant();
        if (shouldRolloutsBeCanceled(distributionSetInvalidation.getActionCancellationType())) {
            final String handlerId = JpaRolloutManagement.createRolloutLockKey(tenant);
            final Lock lock = lockRegistry.obtain(handlerId);
            try {
                if (!lock.tryLock(repositoryProperties.getDsInvalidationLockTimeout(), TimeUnit.SECONDS)) {
                    throw new StopRolloutException("Timeout while trying to invalidate distribution sets");
                }
                try {
                    invalidateDistributionSetsInTransaction(distributionSetInvalidation, tenant);
                } finally {
                    lock.unlock();
                }
            } catch (final InterruptedException e) {
                log.error("InterruptedException while invalidating distribution sets {}!",
                        distributionSetInvalidation.getDistributionSetIds(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            // no lock is needed as no rollout will be stopped
            invalidateDistributionSetsInTransaction(distributionSetInvalidation, tenant);
        }
    }

    private static boolean shouldRolloutsBeCanceled(final ActionCancellationType cancelationType) {
        return cancelationType != ActionCancellationType.NONE;
    }

    private void invalidateDistributionSetsInTransaction(final DistributionSetInvalidation distributionSetInvalidation, final String tenant) {
        DeploymentHelper.runInNewTransaction(txManager, tenant + "-invalidateDS", status -> {
            distributionSetInvalidation.getDistributionSetIds().forEach(setId -> invalidateDistributionSet(setId,
                    distributionSetInvalidation.getActionCancellationType()));

            return 0;
        });
    }

    private void invalidateDistributionSet(final long setId, final ActionCancellationType cancelationType) {
        final DistributionSet distributionSet = distributionSetManagement.get(setId);
        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException(
                    "Distribution set of type " + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }
        distributionSetManagement.invalidate(distributionSet);
        log.debug("Distribution set {} marked as invalid.", setId);

        // rollout cancellation should only be permitted with UPDATE_ROLLOUT permission
        if (shouldRolloutsBeCanceled(cancelationType)) {
            log.debug("Cancel rollouts after ds invalidation. ID: {}", setId);
            rolloutManagement.cancelRolloutsForDistributionSet(distributionSet, cancelationType);
        }

        if (cancelationType != ActionCancellationType.NONE) {
            log.debug("Cancel actions after ds invalidation. ID: {}", setId);
            deploymentManagement.cancelActionsForDistributionSet(cancelationType, distributionSet);
        }

        // Do run as system to ensure all actions (even invisible) are canceled due to invalidation.
        AccessContext.asSystem(() -> {
            log.debug("Cancel auto assignments after ds invalidation. ID: {}", setId);
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(setId);
        });
    }
}