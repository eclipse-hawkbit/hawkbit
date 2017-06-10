/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.repository;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.JpaDeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * This class provides implementation for processing an offline software update
 * in addition to methods provided by {@link JpaDeploymentManagement}.
 */
@Component(value = "OfflineUpdateDeploymentManagementImpl")
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class OfflineUpdateDeploymentManagementImpl extends JpaDeploymentManagement
        implements OfflineUpdateDeploymentManagement {

    private static final Logger LOG = LoggerFactory.getLogger(OfflineUpdateDeploymentManagementImpl.class);

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EntityManager entityManager;

    /**
     * Complete an {@link Action} for the given actionId.
     *
     * @param actionId
     *            id of the action to be updated
     *
     * @return completed {@link Action}.
     */
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public Action finishAction(final Long actionId) {
        LOG.debug("Finishing action {}.", actionId);

        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (action.isActive()) {
            final JpaDistributionSet distributionSet = (JpaDistributionSet) action.getDistributionSet();

            LOG.debug("Action ({}) is active. Updating to {}.", action, Status.FINISHED);
            action.setStatus(Status.FINISHED);
            action.setActive(false);

            Long actionUpdateTime = System.currentTimeMillis();
            actionStatusRepository.save(new JpaActionStatus(action, Status.FINISHED, actionUpdateTime,
                    "Action finished for offline update of distribution set."));

            final Action savedAction = actionRepository.save(action);

            updateTargetInfoForAction(action.getTarget(), distributionSet, actionUpdateTime);

            afterCommit.afterCommit(() -> eventPublisher
                    .publishEvent(new ActionUpdatedEvent(action, null, null, applicationContext.getId())));
            LOG.debug("Finished action {}.", actionId);

            return savedAction;
        } else {
            LOG.warn("Update of actionStatus {} for action {} not possible since action is not active anymore.",
                    action.getStatus(), action.getId());
            return action;
        }
    }

    /**
     * Updates the details of {@link Target}.
     *
     * @param target
     *            whose details are to be updated
     * @param distributionSet
     *            needed to update target details
     * @param distributionSetInstallationTime
     *            time at which the distribution set was installed.
     */
    private void updateTargetInfoForAction(final Target target, final JpaDistributionSet distributionSet,
            Long distributionSetInstallationTime) {
        JpaTarget updatedTarget = (JpaTarget) entityManager.merge(target);

        updatedTarget.setInstalledDistributionSet(distributionSet);
        updatedTarget.setInstallationDate(distributionSetInstallationTime);

        if (updatedTarget.getAssignedDistributionSet() != null && updatedTarget.getAssignedDistributionSet().getId()
                .equals(updatedTarget.getInstalledDistributionSet().getId())) {
            updatedTarget.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
        }

        targetRepository.save(updatedTarget);
        LOG.debug("Updated the target {}.", updatedTarget);

        afterCommit.afterCommit(
                () -> eventPublisher.publishEvent(new TargetUpdatedEvent(updatedTarget, applicationContext.getId())));
    }
}
