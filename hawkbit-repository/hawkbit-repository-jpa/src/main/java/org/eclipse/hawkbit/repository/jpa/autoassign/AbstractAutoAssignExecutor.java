/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;

/**
 * Abstract implementation of an AutoAssignExecutor
 */
@Slf4j
public abstract class AbstractAutoAssignExecutor implements AutoAssignExecutor {

    /**
     * The message which is added to the action status when a distribution set is
     * assigned to an target. First %s is the name of the target filter.
     */
    private static final String ACTION_MESSAGE = "Auto assignment by target filter: %s";

    /**
     * Maximum for target filter queries with auto assign DS activated.
     */
    private static final int PAGE_SIZE = 1000;

    private final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    private final DeploymentManagement deploymentManagement;
    private final PlatformTransactionManager transactionManager;
    private final ContextAware contextAware;

    /**
     * Constructor
     *
     * @param targetFilterQueryManagement to get all target filter queries
     * @param deploymentManagement to assign distribution sets to targets
     * @param transactionManager to run transactions
     * @param contextAware to handle the context
     */
    protected AbstractAutoAssignExecutor(
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager, final ContextAware contextAware) {
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.deploymentManagement = deploymentManagement;
        this.transactionManager = transactionManager;
        this.contextAware = contextAware;
    }

    protected static String getAutoAssignmentInitiatedBy(final TargetFilterQuery targetFilterQuery) {
        return StringUtils.hasText(targetFilterQuery.getAutoAssignInitiatedBy())
                ? targetFilterQuery.getAutoAssignInitiatedBy()
                : targetFilterQuery.getCreatedBy();
    }

    protected DeploymentManagement getDeploymentManagement() {
        return deploymentManagement;
    }

    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    protected TenantAware getContextAware() {
        return contextAware;
    }

    // run in the context the auto assignment is made in, i.e. if there is access control context it runs in it
    // otherwise in the tenant & user context built by createdBy
    // Note! It must be called in a tenant context, i.e. contextAware.getCurrentTenant() returns the tenant
    protected void forEachFilterWithAutoAssignDS(final Consumer<TargetFilterQuery> consumer) {
        Slice<TargetFilterQuery> filterQueries;
        Pageable query = PageRequest.of(0, PAGE_SIZE);

        do {
            filterQueries = targetFilterQueryManagement.findWithAutoAssignDS(query);

            filterQueries.forEach(filterQuery -> {
                try {
                    filterQuery.getAccessControlContext().ifPresentOrElse(
                            context -> // has stored context - executes it with it
                                    contextAware.runInContext(
                                            context,
                                            () -> consumer.accept(filterQuery)),
                            () -> // has no stored context - executes it in the tenant & user scope
                                    contextAware.runAsTenantAsUser(
                                            contextAware.getCurrentTenant(),
                                            getAutoAssignmentInitiatedBy(filterQuery), () -> {
                                                consumer.accept(filterQuery);
                                                return null;
                                            })
                    );
                } catch (final RuntimeException ex) {
                    log.debug(
                            "Exception on forEachFilterWithAutoAssignDS execution for tenant {} with filter id {}. Continue with next filter query.",
                            filterQuery.getTenant(), filterQuery.getId(), ex);
                    log.error(
                            "Exception on forEachFilterWithAutoAssignDS execution for tenant {} with filter id {} and error message [{}]. "
                                    + "Continue with next filter query.",
                            filterQuery.getTenant(), filterQuery.getId(), ex.getMessage());
                }
            });
        } while ((query = filterQueries.nextPageable()) != Pageable.unpaged());
    }

    /**
     * Runs target assignments within a dedicated transaction for a given list of
     * controllerIDs
     *
     * @param targetFilterQuery the target filter query
     * @param controllerIds the controllerIDs
     * @return count of targets
     */
    protected int runTransactionalAssignment(final TargetFilterQuery targetFilterQuery,
            final List<String> controllerIds) {
        final String actionMessage = String.format(ACTION_MESSAGE, targetFilterQuery.getName());

        return DeploymentHelper.runInNewTransaction(getTransactionManager(), "autoAssignDSToTargets",
                Isolation.READ_COMMITTED.value(), status -> {

                    final List<DeploymentRequest> deploymentRequests = mapToDeploymentRequests(controllerIds,
                            targetFilterQuery);

                    final int count = deploymentRequests.size();
                    if (count > 0) {
                        getDeploymentManagement().assignDistributionSets(
                                getAutoAssignmentInitiatedBy(targetFilterQuery), deploymentRequests, actionMessage);
                    }
                    return count;
                });
    }

    /**
     * Creates a list of {@link DeploymentRequest} for given list of controllerIds
     * and {@link TargetFilterQuery}
     *
     * @param controllerIds list of controllerIds
     * @param filterQuery the query the targets have to match
     * @return list of deployment request
     */
    protected List<DeploymentRequest> mapToDeploymentRequests(final List<String> controllerIds,
            final TargetFilterQuery filterQuery) {
        // the action type is set to FORCED per default (when not explicitly
        // specified)
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
}