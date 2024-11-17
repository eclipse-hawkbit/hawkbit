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

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Jpa implementation for {@link DistributionSetInvalidationManagement}
 */
@Slf4j
public class JpaDistributionSetInvalidationManagement implements DistributionSetInvalidationManagement {

    private final DistributionSetManagement distributionSetManagement;
    private final RolloutManagement rolloutManagement;
    private final DeploymentManagement deploymentManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final ActionRepository actionRepository;
    private final PlatformTransactionManager txManager;
    private final RepositoryProperties repositoryProperties;
    private final TenantAware tenantAware;
    private final LockRegistry lockRegistry;
    private final SystemSecurityContext systemSecurityContext;

    public JpaDistributionSetInvalidationManagement(final DistributionSetManagement distributionSetManagement,
            final RolloutManagement rolloutManagement, final DeploymentManagement deploymentManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement, final ActionRepository actionRepository,
            final PlatformTransactionManager txManager, final RepositoryProperties repositoryProperties,
            final TenantAware tenantAware, final LockRegistry lockRegistry,
            final SystemSecurityContext systemSecurityContext) {
        this.distributionSetManagement = distributionSetManagement;
        this.rolloutManagement = rolloutManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.actionRepository = actionRepository;
        this.txManager = txManager;
        this.repositoryProperties = repositoryProperties;
        this.tenantAware = tenantAware;
        this.lockRegistry = lockRegistry;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    public void invalidateDistributionSet(final DistributionSetInvalidation distributionSetInvalidation) {
        log.debug("Invalidate distribution sets {}", distributionSetInvalidation.getDistributionSetIds());
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
                log.error("InterruptedException while invalidating distribution sets {}!",
                        distributionSetInvalidation.getDistributionSetIds(), e);
                Thread.currentThread().interrupt();
            }
        } else {
            // no lock is needed as no rollout will be stopped
            invalidateDistributionSetsInTransaction(distributionSetInvalidation, tenant);
        }
    }

    @Override
    public DistributionSetInvalidationCount countEntitiesForInvalidation(
            final DistributionSetInvalidation distributionSetInvalidation) {
        return systemSecurityContext.runAsSystem(() -> {
            final Collection<Long> setIds = distributionSetInvalidation.getDistributionSetIds();
            final long rolloutsCount = shouldRolloutsBeCanceled(distributionSetInvalidation.getCancelationType(),
                    distributionSetInvalidation.isCancelRollouts()) ? countRolloutsForInvalidation(setIds) : 0;
            final long autoAssignmentsCount = countAutoAssignmentsForInvalidation(setIds);
            final long actionsCount = countActionsForInvalidation(setIds,
                    distributionSetInvalidation.getCancelationType());

            return new DistributionSetInvalidationCount(rolloutsCount, autoAssignmentsCount, actionsCount);
        });
    }

    private static boolean shouldRolloutsBeCanceled(final CancelationType cancelationType,
            final boolean cancelRollouts) {
        return cancelationType != CancelationType.NONE || cancelRollouts;
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
        final DistributionSet distributionSet = distributionSetManagement.getOrElseThrowException(setId);
        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException("Distribution set of type "
                    + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }
        distributionSetManagement.invalidate(distributionSet);
        log.debug("Distribution set {} marked as invalid.", setId);

        // rollout cancellation should only be permitted with UPDATE_ROLLOUT permission
        if (shouldRolloutsBeCanceled(cancelationType, cancelRollouts)) {
            log.debug("Cancel rollouts after ds invalidation. ID: {}", setId);
            rolloutManagement.cancelRolloutsForDistributionSet(distributionSet);
        }

        // Do run as system to ensure all actions (even invisible) are canceled due to invalidation.
        systemSecurityContext.runAsSystem(() -> {
            if (cancelationType != CancelationType.NONE) {
                log.debug("Cancel actions after ds invalidation. ID: {}", setId);
                deploymentManagement.cancelActionsForDistributionSet(cancelationType, distributionSet);
            }

            log.debug("Cancel auto assignments after ds invalidation. ID: {}", setId);
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(setId);
            return null;
        });
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
        return setIds.stream().mapToLong(actionRepository::countByDistributionSetIdAndActiveIsTrue).sum();
    }

    private long countActionsForSoftInvalidation(final Collection<Long> setIds) {
        return setIds.stream()
                .mapToLong(distributionSet -> actionRepository
                        .countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(distributionSet, Status.CANCELING))
                .sum();
    }
}