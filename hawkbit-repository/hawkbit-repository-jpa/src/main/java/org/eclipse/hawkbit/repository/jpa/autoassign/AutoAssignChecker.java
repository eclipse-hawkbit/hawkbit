/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.PersistenceException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks if targets need a new distribution set (DS) based on the target filter
 * queries and assigns the new DS when necessary. First all target filter
 * queries are listed. For every target filter query (TFQ) the auto assign DS is
 * retrieved. All targets get listed per target filter query, that match the TFQ
 * and that don't have the auto assign DS in their action history.
 */
@Slf4j
public class AutoAssignChecker extends AbstractAutoAssignExecutor {

    private final TargetManagement targetManagement;

    /**
     * Instantiates a new auto assign checker
     *
     * @param targetFilterQueryManagement to get all target filter queries
     * @param targetManagement to get targets
     * @param deploymentManagement to assign distribution sets to targets
     * @param transactionManager to run transactions
     * @param contextAware to handle the context
     */
    public AutoAssignChecker(
            final TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager, final ContextAware contextAware) {
        super(targetFilterQueryManagement, deploymentManagement, transactionManager, contextAware);
        this.targetManagement = targetManagement;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAllTargets() {
        log.debug("Auto assign check call for tenant {} started", getContextAware().getCurrentTenant());
        forEachFilterWithAutoAssignDS(this::checkByTargetFilterQueryAndAssignDS);
        log.debug("Auto assign check call for tenant {} finished", getContextAware().getCurrentTenant());
    }

    @Override
    public void checkSingleTarget(String controllerId) {
        log.debug("Auto assign check call for tenant {} and device {} started", getContextAware().getCurrentTenant(), controllerId);
        forEachFilterWithAutoAssignDS(filter -> checkForDevice(controllerId, filter));
        log.debug("Auto assign check call for tenant {} and device {} finished", getContextAware().getCurrentTenant(), controllerId);
    }

    /**
     * Fetches the distribution set, gets all controllerIds and assigns the DS to
     * them. Catches PersistenceException and own exceptions derived from
     * AbstractServerRtException
     *
     * @param targetFilterQuery the target filter query
     */
    private void checkByTargetFilterQueryAndAssignDS(final TargetFilterQuery targetFilterQuery) {
        log.debug("Auto assign check call for tenant {} and target filter query id {} started",
                getContextAware().getCurrentTenant(), targetFilterQuery.getId());
        try {
            int count;
            do {
                final List<String> controllerIds = targetManagement
                        .findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
                                targetFilterQuery.getAutoAssignDistributionSet().getId(), targetFilterQuery.getQuery(),
                                PageRequest.of(0, Constants.MAX_ENTRIES_IN_STATEMENT)
                        )
                        .getContent().stream().map(Target::getControllerId).toList();
                log.debug(
                        "Retrieved {} auto assign targets for tenant {} and target filter query id {}, starting with assignment",
                        controllerIds.size(), getContextAware().getCurrentTenant(), targetFilterQuery.getId());

                count = runTransactionalAssignment(targetFilterQuery, controllerIds);
                log.debug(
                        "Assignment for {} auto assign targets for tenant {} and target filter query id {} finished",
                        controllerIds.size(), getContextAware().getCurrentTenant(), targetFilterQuery.getId());
            } while (count == Constants.MAX_ENTRIES_IN_STATEMENT);
        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check of target filter query id {}", targetFilterQuery.getId(), e);
        }
        log.debug("Auto assign check call for tenant {} and target filter query id {} finished",
                getContextAware().getCurrentTenant(), targetFilterQuery.getId());
    }

    private void checkForDevice(final String controllerId, final TargetFilterQuery targetFilterQuery) {
        log.debug("Auto assign check call for tenant {} and target filter query id {} for device {} started",
                getContextAware().getCurrentTenant(), targetFilterQuery.getId(), controllerId);
        try {
            final boolean controllerIdMatches = targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
                    controllerId, targetFilterQuery.getAutoAssignDistributionSet().getId(),
                    targetFilterQuery.getQuery());

            if (controllerIdMatches) {
                runTransactionalAssignment(targetFilterQuery, Collections.singletonList(controllerId));
            }

        } catch (final PersistenceException | AbstractServerRtException e) {
            log.error("Error during auto assign check of target filter query id {}", targetFilterQuery.getId(), e);
        }
        log.debug("Auto assign check call for tenant {} and target filter query id {} for device {} finished",
                getContextAware().getCurrentTenant(), targetFilterQuery.getId(), controllerId);
    }
}
