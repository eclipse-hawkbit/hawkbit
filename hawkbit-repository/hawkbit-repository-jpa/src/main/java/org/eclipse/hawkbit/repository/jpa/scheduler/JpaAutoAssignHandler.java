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

import static org.eclipse.hawkbit.context.AccessContext.asActor;
import static org.eclipse.hawkbit.context.AccessContext.withSecurityContext;
import static org.eclipse.hawkbit.repository.model.AutoAssignment.AutoAssignStatus.READY;
import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG;
import static org.eclipse.hawkbit.tenancy.DefaultTenantConfiguration.TENANT_TAG_VALUE_PROVIDER;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.AutoAssignHandler;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks if targets need a new distribution set (DS) based on the auto assignments and assigns the new DS when necessary. First all active auto
 * assignments are listed. For every auto assignment, the DS is retrieved. All targets get listed per auto assignment, that match its query and
 * that don't have the auto assign DS in their action history.
 */
@Slf4j
@Service
public class JpaAutoAssignHandler implements AutoAssignHandler {

    /**
     * The message which is added to the action status when a distribution set is assigned to a target.
     * First %s is the name of the auto assignment.
     */
    private static final String ACTION_MESSAGE = "Auto assignment: %s";

    /**
     * Maximum number of active auto assignments fetched per page.
     */
    private static final int PAGE_SIZE = 1000;

    private static final LockTimeoutException LOCK_TIMEOUT_EXCEPTION = new LockTimeoutException("Could not obtain lock for auto assignment");

    private final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement;
    private final TargetManagement<? extends Target> targetManagement;
    private final DeploymentManagement deploymentManagement;
    private final PlatformTransactionManager transactionManager;
    private final LockRegistry<? extends Lock> lockRegistry;
    private final Optional<MeterRegistry> meterRegistry;

    public JpaAutoAssignHandler(
            final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement,
            final TargetManagement<? extends Target> targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager, final LockRegistry<? extends Lock> lockRegistry,
            final Optional<MeterRegistry> meterRegistry) {
        this.autoAssignmentManagement = autoAssignmentManagement;
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.transactionManager = transactionManager;
        this.lockRegistry = lockRegistry;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAll() {
        final long startNano = System.nanoTime();

        final AtomicReference<Lock> lockRef = new AtomicReference<>();
        try {
            forEachAutoAssignment(autoAssignment -> {
                if (lockRef.get() == null) {
                    // only if there are auto assignments to process we try to obtain the lock (on the first one)
                    final Lock lock = lockRegistry.obtain(createAutoAssignmentLockKey(AccessContext.tenant()));
                    if (!lock.tryLock()) {
                        if (log.isTraceEnabled()) {
                            log.trace("Could not obtain lock {}", lock);
                        }
                        throw LOCK_TIMEOUT_EXCEPTION;
                    } else {
                        lockRef.set(lock);

                        log.debug("Start auto-assign handling");
                    }
                }

                final long startNanoPartial = System.nanoTime();
                try {
                    checkByDistributionSet(autoAssignment);
                } finally {
                    meterRegistry // handle single autoAssignment
                            .map(mReg -> mReg.timer(
                                    "hawkbit.autoassign.handle", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get(), "autoAssignment",
                                    String.valueOf(autoAssignment.getId())))
                            .ifPresent(timer -> timer.record(System.nanoTime() - startNanoPartial, TimeUnit.NANOSECONDS));
                }
            });
        } finally {
            final Lock lock = lockRef.get();
            if (lock != null) {
                lock.unlock();

                // only if there is at least one autoAssignment and lock has been obtained then will be measured (as in rollouts)
                meterRegistry // handle single autoAssignment for single target
                        .map(mReg -> mReg.timer(
                                "hawkbit.autoassign.handle.all", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get()))
                        .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
                log.debug("Auto assign check all targets finished");
            }
        }
    }

    @Override
    public void handleSingleTarget(final String controllerId) {
        log.debug("Auto assign check call for device {} started", controllerId);
        final long startNano = System.nanoTime();

        forEachAutoAssignment(autoAssignment -> {
            final long startNanoPartial = System.nanoTime();
            try {
                checkForDevice(controllerId, autoAssignment);
            } finally {
                meterRegistry // handle single autoAssignment for single target
                        .map(mReg -> mReg.timer(
                                "hawkbit.autoassign.handle.single", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get(), "autoAssignment",
                                String.valueOf(autoAssignment.getId())))
                        .ifPresent(timer -> timer.record(System.nanoTime() - startNanoPartial, TimeUnit.NANOSECONDS));
            }
        });

        meterRegistry // handle all single-target auto assignments
                .map(mReg -> mReg.timer(
                        "hawkbit.autoassign.handle.single.all", TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get()))
                .ifPresent(timer -> timer.record(System.nanoTime() - startNano, TimeUnit.NANOSECONDS));
        log.debug("Auto assign check call for device {} finished", controllerId);
    }

    /**
     * Fetches the distribution set, gets all controllerIds and assigns the DS to them. Catches PersistenceException and own exceptions derived
     * from AbstractServerRtException
     *
     * @param autoAssignment the auto assignment
     */
    private void checkByDistributionSet(final AutoAssignment autoAssignment) {
        log.debug("Auto assign check call for id {} started", autoAssignment.getId());
        try {
            int count;
            do {
                final List<String> controllerIds = targetManagement
                        .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
                                autoAssignment.getDistributionSet().getId(), autoAssignment.getTargetFilterQuery(),
                                PageRequest.of(0, Constants.MAX_ENTRIES_IN_STATEMENT))
                        .getContent().stream().map(Target::getControllerId).toList();
                log.debug("Retrieved {} auto assign targets for auto assignment id {}, starting with assignment",
                        controllerIds.size(), autoAssignment.getId());

                count = runTransactionalAssignment(autoAssignment, controllerIds);
                log.debug("Assignment for {} auto assign targets for auto assignment id {} finished",
                        controllerIds.size(), autoAssignment.getId());
            } while (count == Constants.MAX_ENTRIES_IN_STATEMENT);
        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check id {}", autoAssignment.getId(), e);
        }
        log.debug("Auto assign check call with id {} finished", autoAssignment.getId());
    }

    private static String getAutoAssignmentInitiatedBy(final AutoAssignment autoAssignment) {
        return autoAssignment.getCreatedBy();
    }

    // run in the context the auto assignment is made in, i.e. if there is access control context it runs in it
    // otherwise in the tenant & user context built by createdBy
    // Note: It must be called in a tenant context, i.e. Security.getCurrentTenant() returns the tenant
    private void forEachAutoAssignment(final Consumer<AutoAssignment> consumer) {
        Slice<AutoAssignment> autoAssignments;
        Pageable query = PageRequest.of(0, PAGE_SIZE);
        do {
            autoAssignments = autoAssignmentManagement.getActiveAutoAssignments(query);

            try {
                autoAssignments.forEach(autoAssignment -> {
                    try {
                        if (autoAssignment.getStatus() == READY) {
                            final Long startAt = autoAssignment.getStartAt();
                            if (startAt != null && startAt > System.currentTimeMillis()) {
                                return;
                            }
                            autoAssignmentManagement.start(autoAssignment.getId());
                        }
                        autoAssignment.getAccessControlContext().ifPresentOrElse(
                                // has stored context - executes it with it
                                context -> withSecurityContext(context, () -> consumer.accept(autoAssignment)),
                                // has no stored context - executes it in the tenant & user scope
                                () -> asActor(getAutoAssignmentInitiatedBy(autoAssignment), () -> consumer.accept(autoAssignment)));
                    } catch (final RuntimeException ex) {
                        if (ex == LOCK_TIMEOUT_EXCEPTION) {
                            // expected - just stop processing further
                            throw ex; // throw in order to break
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(
                                    "Exception on forEachAutoAssignment execution for auto assignment id {}. Continue with next auto assignment.",
                                    autoAssignment.getId(), ex);
                        } else {
                            log.error(
                                    "Exception on forEachAutoAssignment execution for auto assignment id {} and error message [{}]. Continue with next auto assignment.",
                                    autoAssignment.getId(), ex.getMessage());
                        }
                    }
                });
            } catch (final LockTimeoutException lte) {
                break; // lock not found
            }
        } while (autoAssignments.hasNext() && (query = autoAssignments.nextPageable()) != Pageable.unpaged());
    }

    /**
     * Runs target assignments within a dedicated transaction for a given list of controllerIDs
     *
     * @param autoAssignment the auto assignment
     * @param controllerIds the controllerIDs
     * @return count of targets
     */
    private int runTransactionalAssignment(final AutoAssignment autoAssignment, final List<String> controllerIds) {
        final String actionMessage = String.format(ACTION_MESSAGE, autoAssignment.getName());
        return DeploymentHelper.runInNewTransaction(transactionManager, "autoAssignDSToTargets", Isolation.READ_COMMITTED.value(), status -> {
            final List<DeploymentRequest> deploymentRequests = mapToDeploymentRequests(controllerIds, autoAssignment);
            final int count = deploymentRequests.size();
            if (count > 0) {
                asActor(
                        getAutoAssignmentInitiatedBy(autoAssignment),
                        () -> deploymentManagement.assignDistributionSets(deploymentRequests, actionMessage));
            }
            return count;
        });
    }

    /**
     * Creates a list of {@link DeploymentRequest} for given list of controllerIds and {@link AutoAssignment}
     *
     * @param controllerIds list of controllerIds
     * @param autoAssignment the auto assignment the targets have to match
     * @return list of deployment request
     */
    private List<DeploymentRequest> mapToDeploymentRequests(final List<String> controllerIds, final AutoAssignment autoAssignment) {
        // the action type is set to FORCED per default (when not explicitly specified)
        final Action.ActionType autoAssignActionType = autoAssignment.getActionType() == null
                ? Action.ActionType.FORCED
                : autoAssignment.getActionType();
        return controllerIds.stream()
                .map(controllerId -> DeploymentRequest
                        .builder(controllerId, autoAssignment.getDistributionSet().getId())
                        .actionType(autoAssignActionType).weight(autoAssignment.getWeight().orElse(null))
                        .confirmationRequired(autoAssignment.isConfirmationRequired()).build())
                .toList();
    }

    private void checkForDevice(final String controllerId, final AutoAssignment autoAssignment) {
        log.debug("Auto assign check call for auto assignment id {} for device {} started", autoAssignment.getId(), controllerId);
        try {
            if (targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    controllerId, autoAssignment.getDistributionSet().getId(), autoAssignment.getTargetFilterQuery())) {
                runTransactionalAssignment(autoAssignment, Collections.singletonList(controllerId));
            }
        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check of id {}", autoAssignment.getId(), e);
        }
        log.debug("Auto assign check call for id {} for device {} finished", autoAssignment.getId(), controllerId);
    }

    private static String createAutoAssignmentLockKey(final String tenant) {
        return tenant + "-auto-assign";
    }
}