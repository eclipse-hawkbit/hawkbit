/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import com.google.common.collect.Lists;

/**
 * AbstractDsAssignmentStrategy for online assignments, i.e. managed by hawkBit.
 *
 */
public class OnlineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OnlineDsAssignmentStrategy(final TargetRepository targetRepository,
            final AfterTransactionCommitExecutor afterCommit, final ApplicationEventPublisher eventPublisher,
            final ApplicationContext applicationContext, final ActionRepository actionRepository,
            final ActionStatusRepository actionStatusRepository) {
        super(targetRepository, afterCommit, eventPublisher, applicationContext, actionRepository,
                actionStatusRepository);
    }

    @Override
    void sendAssignmentEvents(final DistributionSet set, final List<JpaTarget> targets,
            final Set<Long> targetIdsCancellList, final Map<String, JpaAction> targetIdsToActions) {

        final List<Action> actions = targets.stream().map(target -> {
            target.setUpdateStatus(TargetUpdateStatus.PENDING);
            sendTargetUpdatedEvent(target);

            return target;
        }).filter(target -> !targetIdsCancellList.contains(target.getId())).map(Target::getControllerId)
                .map(targetIdsToActions::get).collect(Collectors.toList());

        sendTargetAssignDistributionSetEvent(set.getTenant(), set.getId(), actions);

    }

    @Override
    List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long setId) {
        return Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository
                        .findAll(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId)))
                .flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    Set<Long> cancelActiveActions(final List<List<Long>> targetIds) {
        return targetIds.stream().map(this::overrideObsoleteUpdateActions).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    void closeActiveActions(final List<List<Long>> targetIds) {
        targetIds.forEach(this::closeObsoleteUpdateActions);
    }

    @Override
    void updateTargetStatus(final JpaDistributionSet set, final List<List<Long>> targetIds, final String currentUser) {
        targetIds.forEach(tIds -> targetRepository.setAssignedDistributionSetAndUpdateStatus(TargetUpdateStatus.PENDING,
                set, System.currentTimeMillis(), currentUser, tIds));

    }

    @Override
    JpaAction createTargetAction(final Map<String, TargetWithActionType> targetsWithActionMap, final JpaTarget target,
            final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(targetsWithActionMap, target, set);
        result.setStatus(Status.RUNNING);
        return result;
    }

    @Override
    JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus result = super.createActionStatus(action, actionMessage);
        result.setStatus(Status.RUNNING);
        return result;
    }

}
