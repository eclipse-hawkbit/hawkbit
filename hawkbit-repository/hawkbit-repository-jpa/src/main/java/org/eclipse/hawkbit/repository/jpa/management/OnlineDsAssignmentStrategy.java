/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.remote.MultiActionAssignEvent;
import org.eclipse.hawkbit.repository.event.remote.MultiActionCancelEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.springframework.util.CollectionUtils;

/**
 * AbstractDsAssignmentStrategy for online assignments, i.e. managed by hawkBit.
 */
public class OnlineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OnlineDsAssignmentStrategy(final TargetRepository targetRepository,
            final AfterTransactionCommitExecutor afterCommit, final EventPublisherHolder eventPublisherHolder,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final QuotaManagement quotaManagement, final BooleanSupplier multiAssignmentsConfig,
            final BooleanSupplier confirmationFlowConfig, final RepositoryProperties repositoryProperties) {
        super(targetRepository, afterCommit, eventPublisherHolder, actionRepository, actionStatusRepository,
                quotaManagement, multiAssignmentsConfig, confirmationFlowConfig, repositoryProperties);
    }

    public void sendDeploymentEvents(final long distributionSetId, final List<Action> actions) {
        if (isMultiAssignmentsEnabled()) {
            sendDeploymentEvent(actions);
            return;
        }

        final List<Action> filteredActions = getActionsWithoutCancellations(actions);
        if (filteredActions.isEmpty()) {
            return;
        }
        sendTargetAssignDistributionSetEvent(filteredActions.get(0).getTenant(), distributionSetId, filteredActions);
    }

    @Override
    public JpaAction createTargetAction(final String initiatedBy, final TargetWithActionType targetWithActionType,
            final List<JpaTarget> targets, final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(initiatedBy, targetWithActionType, targets, set);
        if (result != null) {
            final boolean confirmationRequired = targetWithActionType.isConfirmationRequired()
                    && result.getTarget().getAutoConfirmationStatus() == null;
            if (isConfirmationFlowEnabled() && confirmationRequired) {
                result.setStatus(Status.WAIT_FOR_CONFIRMATION);
            } else {
                result.setStatus(Status.RUNNING);
            }
        }
        return result;
    }

    /**
     * Will be called to create the initial action status for an action
     */
    @Override
    public JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus result = super.createActionStatus(action, actionMessage);
        if (isConfirmationFlowEnabled()) {
            result.setStatus(Status.WAIT_FOR_CONFIRMATION);
        } else {
            result.setStatus(Status.RUNNING);
        }
        return result;
    }

    @Override
    public List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long setId) {
        final Function<List<String>, List<JpaTarget>> mapper;
        if (isMultiAssignmentsEnabled()) {
            mapper = ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids));
        } else {
            mapper = ids -> targetRepository
                    .findAll(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId));
        }
        return ListUtils.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream().map(mapper)
                .flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets) {
        targets.forEach(target -> {
            target.setUpdateStatus(TargetUpdateStatus.PENDING);
            sendTargetUpdatedEvent(target);
        });
    }

    @Override
    public void setAssignedDistributionSetAndTargetStatus(final JpaDistributionSet set, final List<List<Long>> targetIds,
            final String currentUser) {
        final long now = System.currentTimeMillis();
        targetIds.forEach(targetIdsChunk -> {
            if (targetRepository.count(AccessController.Operation.UPDATE,
                    targetRepository.byIdsSpec(targetIdsChunk)) != targetIdsChunk.size()) {
                throw new InsufficientPermissionException("No update access to all targets!");
            }
            targetRepository.setAssignedDistributionSetAndUpdateStatus(TargetUpdateStatus.PENDING,
                    set, now, currentUser, targetIdsChunk);
            // TODO AC - current problem with this approach is that the caller detach the targets and seems doesn't save them
//            targetRepository.saveAll(
//                targetRepository
//                        .findAll(AccessController.Operation.UPDATE, targetRepository.byIdsSpec(targetIdsChunk))
//                        .stream()
//                        .map(target -> {
//                            target.setAssignedDistributionSet(set);
//                            target.setLastModifiedAt(now);
//                            target.setLastModifiedBy(currentUser);
//                            target.setUpdateStatus(TargetUpdateStatus.PENDING);
//                            return target;
//                        })
//                        .toList());
        });
    }

    @Override
    public Set<Long> cancelActiveActions(final List<List<Long>> targetIds) {
        return targetIds.stream().map(this::overrideObsoleteUpdateActions).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void closeActiveActions(final List<List<Long>> targetIds) {
        targetIds.forEach(this::closeObsoleteUpdateActions);
    }

    @Override
    public void sendDeploymentEvents(final DistributionSetAssignmentResult assignmentResult) {
        if (isMultiAssignmentsEnabled()) {
            sendDeploymentEvents(Collections.singletonList(assignmentResult));
        } else {
            sendDistributionSetAssignedEvent(assignmentResult);
        }
    }

    @Override
    public void sendDeploymentEvents(final List<DistributionSetAssignmentResult> assignmentResults) {
        if (isMultiAssignmentsEnabled()) {
            sendDeploymentEvent(assignmentResults.stream().flatMap(result -> result.getAssignedEntity().stream())
                    .collect(Collectors.toList()));
        } else {
            assignmentResults.forEach(this::sendDistributionSetAssignedEvent);
        }
    }

    void cancelAssignment(final JpaAction action) {
        if (isMultiAssignmentsEnabled()) {
            sendMultiActionCancelEvent(action);
        } else {
            cancelAssignDistributionSetEvent(action);
        }
    }

    private static Stream<Action> filterCancellations(final List<Action> actions) {
        return actions.stream().filter(action -> {
            final Status actionStatus = action.getStatus();
            return Status.CANCELING != actionStatus && Status.CANCELED != actionStatus;
        });
    }

    private static List<Action> getActionsWithoutCancellations(final List<Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }
        return filterCancellations(actions).collect(Collectors.toList());
    }

    private void sendMultiActionCancelEvent(final Action action) {
        sendMultiActionCancelEvent(action.getTenant(), Collections.singletonList(action));
    }

    private void sendDeploymentEvent(final List<Action> actions) {
        final List<Action> filteredActions = getActionsWithoutCancellations(actions);
        if (filteredActions.isEmpty()) {
            return;
        }
        final String tenant = filteredActions.get(0).getTenant();
        sendMultiActionAssignEvent(tenant, filteredActions);
    }

    private DistributionSetAssignmentResult sendDistributionSetAssignedEvent(
            final DistributionSetAssignmentResult assignmentResult) {
        final List<Action> filteredActions = filterCancellations(assignmentResult.getAssignedEntity())
                .collect(Collectors.toList());
        final DistributionSet set = assignmentResult.getDistributionSet();
        sendTargetAssignDistributionSetEvent(set.getTenant(), set.getId(), filteredActions);
        return assignmentResult;
    }

    private void sendTargetAssignDistributionSetEvent(final String tenant, final long distributionSetId,
            final List<Action> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new TargetAssignDistributionSetEvent(tenant, distributionSetId, actions,
                        eventPublisherHolder.getApplicationId(), actions.get(0).isMaintenanceWindowAvailable())));
    }

    /**
     * Helper to fire a {@link MultiActionCancelEvent}. This method may only be
     * called if the Multi-Assignments feature is enabled.
     *
     * @param tenant the event is scoped to
     * @param actions assigned to the targets
     */
    private void sendMultiActionCancelEvent(final String tenant, final List<Action> actions) {
        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new MultiActionCancelEvent(tenant, eventPublisherHolder.getApplicationId(), actions)));
    }

    /**
     * Helper to fire a {@link MultiActionAssignEvent}. This method may only be
     * called if the Multi-Assignments feature is enabled.
     *
     * @param tenant the event is scoped to
     * @param actions assigned to the targets
     */
    private void sendMultiActionAssignEvent(final String tenant, final List<Action> actions) {
        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher()
                .publishEvent(new MultiActionAssignEvent(tenant, eventPublisherHolder.getApplicationId(), actions)));
    }

}
