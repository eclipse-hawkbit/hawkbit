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

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.ListUtils;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
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
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;

/**
 * AbstractDsAssignmentStrategy for offline assignments, i.e. not managed by hawkBit.
 */
class OfflineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OfflineDsAssignmentStrategy(
            final TargetRepository targetRepository,
            final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository,
            final QuotaManagement quotaManagement,
            final BooleanSupplier confirmationFlowConfig, final RepositoryProperties repositoryProperties,
            final Consumer<MaxAssignmentsExceededInfo> maxAssignmentExceededHandler) {
        super(targetRepository, actionRepository, actionStatusRepository,
                quotaManagement, confirmationFlowConfig, repositoryProperties, maxAssignmentExceededHandler);
    }

    @Override
    public JpaAction createTargetAction(
            final TargetWithActionType targetWithActionType, final List<JpaTarget> targets, final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(targetWithActionType, targets, set);
        if (result != null) {
            result.setStatus(Status.FINISHED);
            result.setActive(Boolean.FALSE);
        }
        return result;
    }

    @Override
    public JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus result = super.createActionStatus(action, actionMessage);
        result.setStatus(Status.FINISHED);
        result.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Action reported as offline deployment");
        return result;
    }

    @Override
    public List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long setId) {
        final Function<List<String>, List<JpaTarget>> mapper =
                ids -> targetRepository.findAll(JpaManagementHelper.combineWithAnd(List.of(
                        TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId),
                        TargetSpecifications.notEqualToTargetUpdateStatus(TargetUpdateStatus.PENDING))));

        return ListUtils.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream().map(mapper).flatMap(List::stream).toList();
    }

    @Override
    public void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets) {
        targets.forEach(target -> {
            target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
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
            targetRepository.setAssignedAndInstalledDistributionSetAndUpdateStatus(
                    TargetUpdateStatus.IN_SYNC, set, now, AccessContext.actor(), targetIdsChunk);
            // TODO AC - current problem with this approach is that the caller detach the targets and seems doesn't save them
//            targetRepository.saveAll(
//                    targetRepository
//                            .findAll(AccessController.Operation.UPDATE, targetRepository.byIdsSpec(targetIdsChunk))
//                            .stream()
//                            .map(target -> {
//                                target.setAssignedDistributionSet(set);
//                                target.setInstalledDistributionSet(set);
//                                target.setInstallationDate(now);
//                                target.setLastModifiedAt(now);
//                                target.setLastModifiedBy(currentUser);
//                                target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
//                                return target;
//                            })
//                            .toList());
        });
    }

    @Override
    public void cancelActiveActions(final List<List<Long>> targetIds) {
        // Not supported by offline case
    }

    @Override
    public void closeActiveActions(final List<List<Long>> targetIds) {
        // Not supported by offline case
    }

    @Override
    void sendDeploymentEvents(final List<DistributionSetAssignmentResult> assignmentResults) {
        // no need to send deployment events in the offline case
    }
}