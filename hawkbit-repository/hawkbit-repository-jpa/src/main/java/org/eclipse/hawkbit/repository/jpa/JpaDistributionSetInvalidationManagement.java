/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Jpa implementation for {@link DistributionSetInvalidationManagement}
 *
 */
public class JpaDistributionSetInvalidationManagement implements DistributionSetInvalidationManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaDistributionSetInvalidationManagement.class);

    private final DistributionSetManagement distributionSetManagement;
    private final RolloutManagement rolloutManagement;
    private final DeploymentManagement deploymentManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final PlatformTransactionManager txManager;
    private final RepositoryProperties repositoryProperties;
    private final TenantAware tenantAware;
    private final LockRegistry lockRegistry;

    protected JpaDistributionSetInvalidationManagement(final DistributionSetManagement distributionSetManagement,
            final RolloutManagement rolloutManagement, final DeploymentManagement deploymentManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final PlatformTransactionManager txManager,
            final RepositoryProperties repositoryProperties, final TenantAware tenantAware,
            final LockRegistry lockRegistry) {
        this.distributionSetManagement = distributionSetManagement;
        this.rolloutManagement = rolloutManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.txManager = txManager;
        this.repositoryProperties = repositoryProperties;
        this.tenantAware = tenantAware;
        this.lockRegistry = lockRegistry;
    }

    @Override
    public void invalidateDistributionSet(final DistributionSetInvalidation distributionSetInvalidation) {
        LOG.debug("Invalidate distribution sets {}", distributionSetInvalidation.getDistributionSetIds());
        final String tenant = tenantAware.getCurrentTenant();

        if (shouldRolloutsBeCanceled(distributionSetInvalidation.getCancelationType(),
                distributionSetInvalidation.isCancelRollouts())) {
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
                LOG.error("InterruptedException while invalidating distribution sets {}!",
                        distributionSetInvalidation.getDistributionSetIds(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            // no lock is needed as no rollout will be stopped
            invalidateDistributionSetsInTransaction(distributionSetInvalidation, tenant);
        }
    }

    private void invalidateDistributionSetsInTransaction(final DistributionSetInvalidation distributionSetInvalidation,
            final String tenant) {
        DeploymentHelper.runInNewTransaction(txManager, tenant + "-invalidateDS", status -> {
            distributionSetInvalidation.getDistributionSetIds().forEach(setId -> invalidateDistributionSet(setId,
                    distributionSetInvalidation.getCancelationType(), distributionSetInvalidation.isCancelRollouts()));
            return 0;
        });
    }

    private void invalidateDistributionSet(final long setId, final CancelationType cancelationType,
            final boolean cancelRollouts) {
        final DistributionSet set = distributionSetManagement.getValidAndComplete(setId);
        distributionSetManagement.invalidate(set);
        LOG.debug("Distribution set {} set to invalid", setId);

        if (shouldRolloutsBeCanceled(cancelationType, cancelRollouts)) {
            rolloutManagement.cancelRolloutsForDistributionSet(set);
        }

        if (cancelationType != CancelationType.NONE) {
            deploymentManagement.cancelActionsForDistributionSet(cancelationType, set);
        }

        targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(setId);
    }

    private static boolean shouldRolloutsBeCanceled(final CancelationType cancelationType,
            final boolean cancelRollouts) {
        return cancelationType != CancelationType.NONE || cancelRollouts;
    }

    @Override
    public DistributionSetInvalidationCount countEntitiesForInvalidation(
            final DistributionSetInvalidation distributionSetInvalidation) {
        final Collection<Long> setIds = distributionSetInvalidation.getDistributionSetIds();
        final long rolloutsCount = shouldRolloutsBeCanceled(distributionSetInvalidation.getCancelationType(),
                distributionSetInvalidation.isCancelRollouts()) ? countRolloutsForInvalidation(setIds) : 0;
        final long autoAssignmentsCount = countAutoAssignmentsForInvalidation(setIds);
        final long actionsCount = countActionsForInvalidation(setIds, distributionSetInvalidation.getCancelationType());

        return new DistributionSetInvalidationCount(rolloutsCount, autoAssignmentsCount, actionsCount);
    }

    private long countRolloutsForInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(rolloutManagement::countByDistributionSetIdAndRolloutIsStoppable).sum();
    }

    private long countAutoAssignmentsForInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(targetFilterQueryManagement::countByAutoAssignDistributionSetId).sum();
    }

    private long countActionsForInvalidation(final Collection<Long> setIds, final CancelationType cancelationType) {
        long affectedActionsByDSInvalidation = 0;
        if (cancelationType == CancelationType.FORCE) {
            affectedActionsByDSInvalidation = countActionsForForcedInvalidation(setIds);
        } else if (cancelationType == CancelationType.SOFT) {
            affectedActionsByDSInvalidation = countActionsForSoftInvalidation(setIds);
        }
        return affectedActionsByDSInvalidation;
    }

    private long countActionsForForcedInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(deploymentManagement::countActionsByDistributionSetIdAndActiveIsTrue).sum();
    }

    private long countActionsForSoftInvalidation(final Collection<Long> setIds) {
        return setIds.stream().mapToLong(distributionSet -> deploymentManagement
                .countActionsByDistributionSetIdAndActiveIsTrueAndStatusIsNot(distributionSet, Status.CANCELING)).sum();
    }

}
