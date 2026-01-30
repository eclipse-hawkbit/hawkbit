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

import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.management.JpaDeploymentManagement.MaxAssignmentsExceededInfo;
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
import org.springframework.util.CollectionUtils;

/**
 * AbstractDsAssignmentStrategy for online assignments, i.e. managed by hawkBit.
 */
class OnlineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OnlineDsAssignmentStrategy(
            final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final QuotaManagement quotaManagement,
            final BooleanSupplier confirmationFlowConfig, final RepositoryProperties repositoryProperties,
            final Consumer<MaxAssignmentsExceededInfo> maxAssignmentExceededHandler) {
        super(targetRepository, actionRepository, actionStatusRepository,
                quotaManagement, confirmationFlowConfig, repositoryProperties, maxAssignmentExceededHandler);
    }

    public void sendDeploymentEvents(final long distributionSetId, final List<Action> actions) {
        final List<Action> filteredActions = getActionsWithoutCancellations(actions);
        if (filteredActions.isEmpty()) {
            return;
        }
        sendTargetAssignDistributionSetEvent(filteredActions.get(0).getTenant(), distributionSetId, filteredActions);
    }

    @Override
    public JpaAction createTargetAction(final TargetWithActionType targetWithActionType,
            final List<JpaTarget> targets, final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(targetWithActionType, targets, set);
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
        final Function<List<String>, List<JpaTarget>> mapper =
                ids -> targetRepository.findAll(
                        TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId));
        return ListUtils.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream().map(mapper)
                .flatMap(List::stream).toList();
    }

    @Override
    public void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets) {
        targets.forEach(target -> {
            target.setUpdateStatus(TargetUpdateStatus.PENDING);
            sendTargetUpdatedEvent(target);
        });
    }

    @Override
    public void setAssignedDistributionSetAndTargetStatus(final JpaDistributionSet set, final List<List<Long>> targetIds) {
        final long now = System.currentTimeMillis();
        targetIds.forEach(targetIdsChunk -> {
            if (targetRepository.count(AccessController.Operation.UPDATE,
                    targetRepository.byIdsSpec(targetIdsChunk)) != targetIdsChunk.size()) {
                throw new InsufficientPermissionException("No update access to all targets!");
            }
            targetRepository.setAssignedDistributionSetAndUpdateStatus(
                    set, now, AccessContext.actor(), TargetUpdateStatus.PENDING, targetIdsChunk);
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
    public void cancelActiveActions(final List<List<Long>> targetIds) {
        targetIds.forEach(this::overrideObsoleteUpdateActions);
    }

    @Override
    public void closeActiveActions(final List<List<Long>> targetIds) {
        targetIds.forEach(this::closeObsoleteUpdateActions);
    }

    @Override
    public void sendDeploymentEvents(final List<DistributionSetAssignmentResult> assignmentResults) {
        assignmentResults.forEach(this::sendDistributionSetAssignedEvent);
    }

    void sendCancellationMessage(final JpaAction action) {
        cancelAssignDistributionSetEvent(action);
    }

    void sendCancellationMessages(final List<JpaAction> actions) {
        actions.forEach(this::cancelAssignDistributionSetEvent);
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
        return filterCancellations(actions).toList();
    }

    private void sendDistributionSetAssignedEvent(final DistributionSetAssignmentResult assignmentResult) {
        final List<Action> filteredActions = filterCancellations(assignmentResult.getAssignedEntity()).toList();
        final DistributionSet set = assignmentResult.getDistributionSet();
        sendTargetAssignDistributionSetEvent(set.getTenant(), set.getId(), filteredActions);
    }

    private void sendTargetAssignDistributionSetEvent(final String tenant, final long distributionSetId,
            final List<Action> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        afterCommit(() -> EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new TargetAssignDistributionSetEvent(tenant, distributionSetId, actions, actions.get(0).isMaintenanceWindowAvailable())));
    }
}