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

import static org.eclipse.hawkbit.audit.HawkbitAuditorAware.runAsAuditor;
import static org.eclipse.hawkbit.context.ContextAware.runInContext;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.PersistenceException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.ContextAware;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Checks if targets need a new distribution set (DS) based on the target filter queries and assigns the new DS when necessary. First all target
 * filter queries are listed. For every target filter query (TFQ) the auto assign DS is retrieved. All targets get listed per target filter
 * query, that match the TFQ and that don't have the auto assign DS in their action history.
 */
@Slf4j
@Service
public class JpaAutoAssignExecutor implements AutoAssignExecutor {

    /**
     * The message which is added to the action status when a distribution set is assigned to a target.
     * First %s is the name of the target filter.
     */
    private static final String ACTION_MESSAGE = "Auto assignment by target filter: %s";

    /**
     * Maximum for target filter queries with auto assign DS activated.
     */
    private static final int PAGE_SIZE = 1000;

    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final TargetManagement<? extends Target> targetManagement;
    private final DeploymentManagement deploymentManagement;
    private final PlatformTransactionManager transactionManager;

    public JpaAutoAssignExecutor(
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final TargetManagement<? extends Target> targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager) {
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.transactionManager = transactionManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAllTargets() {
        log.debug("Auto assign check call started");
        forEachFilterWithAutoAssignDS(this::checkByTargetFilterQueryAndAssignDS);
        log.debug("Auto assign check call finished");
    }

    @Override
    public void checkSingleTarget(final String controllerId) {
        log.debug("Auto assign check call for device {} started", controllerId);
        forEachFilterWithAutoAssignDS(filter -> checkForDevice(controllerId, filter));
        log.debug("Auto assign check call for device {} finished", controllerId);
    }

    /**
     * Fetches the distribution set, gets all controllerIds and assigns the DS to them. Catches PersistenceException and own exceptions derived
     * from AbstractServerRtException
     *
     * @param targetFilterQuery the target filter query
     */
    private void checkByTargetFilterQueryAndAssignDS(final TargetFilterQuery targetFilterQuery) {
        log.debug("Auto assign check call for target filter query id {} started", targetFilterQuery.getId());
        try {
            int count;
            do {
                final List<String> controllerIds = targetManagement
                        .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
                                targetFilterQuery.getAutoAssignDistributionSet().getId(), targetFilterQuery.getQuery(),
                                PageRequest.of(0, Constants.MAX_ENTRIES_IN_STATEMENT))
                        .getContent().stream().map(Target::getControllerId).toList();
                log.debug("Retrieved {} auto assign targets for target filter query id {}, starting with assignment",
                        controllerIds.size(), targetFilterQuery.getId());

                count = runTransactionalAssignment(targetFilterQuery, controllerIds);
                log.debug("Assignment for {} auto assign targets for target filter query id {} finished",
                        controllerIds.size(), targetFilterQuery.getId());
            } while (count == Constants.MAX_ENTRIES_IN_STATEMENT);
        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check of target filter query id {}", targetFilterQuery.getId(), e);
        }
        log.debug("Auto assign check call for target filter query id {} finished", targetFilterQuery.getId());
    }

    private static String getAutoAssignmentInitiatedBy(final TargetFilterQuery targetFilterQuery) {
        return StringUtils.hasText(targetFilterQuery.getAutoAssignInitiatedBy())
                ? targetFilterQuery.getAutoAssignInitiatedBy()
                : targetFilterQuery.getCreatedBy();
    }

    // run in the context the auto assignment is made in, i.e. if there is access control context it runs in it
    // otherwise in the tenant & user context built by createdBy
    // Note: It must be called in a tenant context, i.e. ContextAware.getCurrentTenant() returns the tenant
    private void forEachFilterWithAutoAssignDS(final Consumer<TargetFilterQuery> consumer) {
        Slice<TargetFilterQuery> filterQueries;
        Pageable query = PageRequest.of(0, PAGE_SIZE);
        do {
            filterQueries = targetFilterQueryManagement.findWithAutoAssignDS(query);

            filterQueries.forEach(filterQuery -> {
                try {
                    filterQuery.getAccessControlContext().ifPresentOrElse(
                            context -> // has stored context - executes it with it
                                    runInContext(context, () -> consumer.accept(filterQuery)),
                            () -> // has no stored context - executes it in the tenant & user scope
                                    runAsAuditor(getAutoAssignmentInitiatedBy(filterQuery), () -> consumer.accept(filterQuery))
                    );
                } catch (final RuntimeException ex) {
                    if (log.isDebugEnabled()) {
                        log.debug("Exception on forEachFilterWithAutoAssignDS execution for filter id {}. Continue with next filter query.",
                                filterQuery.getId(), ex);
                    } else {
                        log.error(
                                "Exception on forEachFilterWithAutoAssignDS execution for filter id {} and error message [{}]. Continue with next filter query.",
                                filterQuery.getId(), ex.getMessage());
                    }
                }
            });
        } while (filterQueries.hasNext() && (query = filterQueries.nextPageable()) != Pageable.unpaged());
    }

    /**
     * Runs target assignments within a dedicated transaction for a given list of controllerIDs
     *
     * @param targetFilterQuery the target filter query
     * @param controllerIds the controllerIDs
     * @return count of targets
     */
    private int runTransactionalAssignment(final TargetFilterQuery targetFilterQuery, final List<String> controllerIds) {
        final String actionMessage = String.format(ACTION_MESSAGE, targetFilterQuery.getName());
        return DeploymentHelper.runInNewTransaction(transactionManager, "autoAssignDSToTargets", Isolation.READ_COMMITTED.value(), status -> {
            final List<DeploymentRequest> deploymentRequests = mapToDeploymentRequests(controllerIds, targetFilterQuery);
            final int count = deploymentRequests.size();
            if (count > 0) {
                deploymentManagement.assignDistributionSets(getAutoAssignmentInitiatedBy(targetFilterQuery), deploymentRequests, actionMessage);
            }
            return count;
        });
    }

    /**
     * Creates a list of {@link DeploymentRequest} for given list of controllerIds and {@link TargetFilterQuery}
     *
     * @param controllerIds list of controllerIds
     * @param filterQuery the query the targets have to match
     * @return list of deployment request
     */
    private List<DeploymentRequest> mapToDeploymentRequests(final List<String> controllerIds, final TargetFilterQuery filterQuery) {
        // the action type is set to FORCED per default (when not explicitly specified)
        final Action.ActionType autoAssignActionType = filterQuery.getAutoAssignActionType() == null
                ? Action.ActionType.FORCED
                : filterQuery.getAutoAssignActionType();
        return controllerIds.stream()
                .map(controllerId -> DeploymentRequest
                        .builder(controllerId, filterQuery.getAutoAssignDistributionSet().getId())
                        .actionType(autoAssignActionType).weight(filterQuery.getAutoAssignWeight().orElse(null))
                        .confirmationRequired(filterQuery.isConfirmationRequired()).build())
                .toList();
    }

    private void checkForDevice(final String controllerId, final TargetFilterQuery targetFilterQuery) {
        log.debug("Auto assign check call for target filter query id {} for device {} started", targetFilterQuery.getId(), controllerId);
        try {
            if (targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    controllerId, targetFilterQuery.getAutoAssignDistributionSet().getId(), targetFilterQuery.getQuery())) {
                runTransactionalAssignment(targetFilterQuery, Collections.singletonList(controllerId));
            }
        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check of target filter query id {}", targetFilterQuery.getId(), e);
        }
        log.debug("Auto assign check call for target filter query id {} for device {} finished", targetFilterQuery.getId(), controllerId);
    }
}