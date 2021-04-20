/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.autoassign.AutoAssignExecutor;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractAutoAssignExecutor implements AutoAssignExecutor {

    /**
     * The message which is added to the action status when a distribution set is
     * assigned to an target. First %s is the name of the target filter.
     */
    private static final String ACTION_MESSAGE = "Auto assignment by target filter: %s";

    private final TargetFilterQueryManagement targetFilterQueryManagement;

    private final TargetManagement targetManagement;

    private final DeploymentManagement deploymentManagement;

    private final PlatformTransactionManager transactionManager;

    private final TenantAware tenantAware;

    /**
     * Constructor
     * 
     * @param targetFilterQueryManagement
     *            to get all target filter queries
     * @param targetManagement
     *            to get targets
     * @param deploymentManagement
     *            to assign distribution sets to targets
     * @param transactionManager
     *            to run transactions
     * @param tenantAware
     *            to handle the tenant context
     */
    protected AbstractAutoAssignExecutor(final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager, final TenantAware tenantAware) {
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.transactionManager = transactionManager;
        this.tenantAware = tenantAware;
    }

    /**
     * Runs one page of target assignments within a dedicated transaction
     *
     * @param targetFilterQuery
     *            the target filter query
     * @return count of targets
     */
    protected int runTransactionalAssignment(final TargetFilterQuery targetFilterQuery, final Pageable pageRequest) {
        final String actionMessage = String.format(ACTION_MESSAGE, targetFilterQuery.getName());

        return DeploymentHelper.runInNewTransaction(transactionManager, "autoAssignDSToTargets",
                Isolation.READ_COMMITTED.value(), status -> {
                    final List<DeploymentRequest> deploymentRequests = createAssignmentRequests(targetFilterQuery,
                            pageRequest);
                    final int count = deploymentRequests.size();
                    if (count > 0) {
                        deploymentManagement.assignDistributionSets(getAutoAssignmentInitiatedBy(targetFilterQuery),
                                deploymentRequests, actionMessage);
                    }
                    return count;
                });
    }

    /**
     * Gets all matching targets with the designated action from the target
     * management
     *
     * @param targetFilterQuery
     *            the query the targets have to match
     * @return list of targets with action type
     */
    protected List<DeploymentRequest> createAssignmentRequests(final TargetFilterQuery targetFilterQuery,
            final Pageable pageRequest) {
        final Long dsId = targetFilterQuery.getAutoAssignDistributionSet().getId();
        final Action.ActionType type = targetFilterQuery.getAutoAssignActionType();
        final Page<Target> targets = targetManagement.findByTargetFilterQueryAndNonDS(pageRequest, dsId,
                targetFilterQuery.getQuery());
        // the action type is set to FORCED per default (when not explicitly
        // specified)
        final Action.ActionType autoAssignActionType = type == null ? Action.ActionType.FORCED : type;

        return targets.getContent().stream()
                .map(t -> DeploymentManagement.deploymentRequest(t.getControllerId(), dsId)
                        .setActionType(autoAssignActionType)
                        .setWeight(targetFilterQuery.getAutoAssignWeight().orElse(null)).build())
                .collect(Collectors.toList());
    }

    protected Page<TargetFilterQuery> findWithAutoAssignDS(final Pageable pageRequest) {
        return targetFilterQueryManagement.findWithAutoAssignDS(pageRequest);
    }

    protected void runInUserContext(final TargetFilterQuery targetFilterQuery, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler,
                () -> Objects.requireNonNull(getAutoAssignmentInitiatedBy(targetFilterQuery)), tenantAware);
    }

    protected static String getAutoAssignmentInitiatedBy(final TargetFilterQuery targetFilterQuery) {
        return StringUtils.isEmpty(targetFilterQuery.getAutoAssignInitiatedBy()) ? targetFilterQuery.getCreatedBy()
                : targetFilterQuery.getAutoAssignInitiatedBy();
    }
}
