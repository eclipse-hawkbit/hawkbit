/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.eclipse.hawkbit.auth.SpPermission.CREATE_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_ROLLOUT;
import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_READ_REPOSITORY;
import static org.eclipse.hawkbit.auth.SpringEvalExpressions.HAS_UPDATE_REPOSITORY;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionCancellationType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * A DeploymentManagement service provides operations for the deployment of {@link DistributionSet}s to {@link Target}s.
 */
public interface DeploymentManagement extends PermissionSupport {

    String HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET = HAS_UPDATE_REPOSITORY + " and hasPermission(#root, 'READ_" + DISTRIBUTION_SET + "')";
    String HAS_UPDATE_TARGET_OR_CREATE_UPDATE_ROLLOUT = HAS_UPDATE_REPOSITORY + " or hasPermission(#root, '" + CREATE_ROLLOUT + "')" + " or hasPermission(#root, '" + UPDATE_ROLLOUT + "')";

    @Override
    default String permissionGroup() {
        return SpPermission.TARGET;
    }

    /**
     * Assigns {@link DistributionSet}s to {@link Target}s according to the {@link DeploymentRequest}.
     *
     * @param deploymentRequests information about all target-ds-assignments that shall be made
     * @return the list of assignment results
     * @throws IncompleteDistributionSetException if mandatory {@link SoftwareModuleType} are not assigned as
     *         defined by the {@link DistributionSetType}.
     * @throws EntityNotFoundException if either provided {@link DistributionSet} or {@link Target}s do not exist
     * @throws AssignmentQuotaExceededException if the maximum number of targets the distribution set can be
     *         assigned to at once is exceeded
     * @throws MultiAssignmentIsNotEnabledException if the request results in multiple assignments to the same
     *         target and multi-assignment is disabled
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    List<DistributionSetAssignmentResult> assignDistributionSets(@Valid @NotEmpty List<DeploymentRequest> deploymentRequests);

    /**
     * Assigns {@link DistributionSet}s to {@link Target}s according to the {@link DeploymentRequest}.
     *
     * @param initiatedBy the username of the user who initiated the assignment
     * @param deploymentRequests information about all target-ds-assignments that shall be made
     * @param actionMessage an optional message for the action status
     * @return the list of assignment results
     * @throws IncompleteDistributionSetException if mandatory {@link SoftwareModuleType} are not assigned as
     *         defined by the {@link DistributionSetType}.
     * @throws EntityNotFoundException if either provided {@link DistributionSet} or {@link Target}s do not exist
     * @throws AssignmentQuotaExceededException if the maximum number of targets the distribution set can be
     *         assigned to at once is exceeded
     * @throws MultiAssignmentIsNotEnabledException if the request results in multiple assignments to the same
     *         target and multi-assignment is disabled
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    List<DistributionSetAssignmentResult> assignDistributionSets(
            String initiatedBy, @Valid @NotEmpty List<DeploymentRequest> deploymentRequests, String actionMessage);

    /**
     * Registers "offline" assignments. "offline" assignment means adding a completed action for a {@link DistributionSet} to a {@link Target}.
     * The handling differs to hawkBit-managed updates by means that:<br/>
     *
     * <ol type="A">
     * <li>it ignores targets completely that are in
     * {@link TargetUpdateStatus#PENDING}.</li>
     * <li>it creates completed actions.</li>
     * <li>sets both installed and assigned DS on the target and switches the
     * status to {@link TargetUpdateStatus#IN_SYNC}.</li>
     * <li>does not send a {@link TargetAssignDistributionSetEvent}.</li>
     * </ol>
     *
     * @param assignments target IDs with the respective distribution set ID which they are supposed to be assigned to
     * @return the assignment results
     * @throws IncompleteDistributionSetException if mandatory {@link SoftwareModuleType} are not assigned as
     *         defined by the {@link DistributionSetType}.
     * @throws EntityNotFoundException if either provided {@link DistributionSet} or {@link Target}s do not exist
     * @throws AssignmentQuotaExceededException if the maximum number of targets the distribution set can be assigned to at once is exceeded
     * @throws MultiAssignmentIsNotEnabledException if the request results in multiple assignments to the same
     *         target and multi-assignment is disabled
     */
    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(String initiatedBy, Collection<Entry<String, Long>> assignments);

    @PreAuthorize(HAS_UPDATE_TARGET_AND_READ_DISTRIBUTION_SET)
    List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(Collection<Entry<String, Long>> assignments);

    /**
     * Cancels the {@link Action} with the given ID. The method will immediately add a {@link Status#CANCELED} status to the action.
     * However, it might be possible that the controller will continue to work on the cancellation.
     *
     * @param actionId to be canceled
     * @return canceled {@link Action}
     * @throws CancelActionNotAllowedException in case the given action is not active or is already a cancel action
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    Action cancelAction(long actionId);

    /**
     * Counts all actions associated to a specific target.
     *
     * @param rsql rsql query string
     * @param controllerId the target associated to the actions to count
     * @return the count value of found actions associated to the target
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countActionsByTarget(@NotNull String rsql, @NotEmpty String controllerId);

    /**
     * Returns total count of all actions
     *
     * @return the total amount of stored actions
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countActionsAll();

    /**
     * Counts the actions which match the given query.
     *
     * @param rsql RSQL query.
     * @return the total number of actions matching the given RSQL query.
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countActions(@NotNull String rsql);

    /**
     * Counts all actions associated to a specific target.
     *
     * @param controllerId the target associated to the actions to count
     * @return the count value of found actions associated to the target
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    long countActionsByTarget(@NotEmpty String controllerId);

    /**
     * Get the {@link Action} entity for given actionId.
     *
     * @param actionId to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Optional<Action> findAction(long actionId);

    /**
     * Retrieves all {@link Action}s from repository.
     *
     * @param pageable pagination parameter
     * @return a paged list of {@link Action}s
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Slice<Action> findActionsAll(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link Action} entities which match the given RSQL query.
     *
     * @param rsql RSQL query string
     * @param pageable the page request parameter for paging and sorting the result
     * @return a paged list of {@link Action}s.
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Slice<Action> findActions(@NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves all {@link Action}s assigned to a specific {@link Target} and a given specification.
     *
     * @param rsql rsql query string
     * @param controllerId the target which must be assigned to the actions
     * @param pageable the page request
     * @return a slice of actions assigned to the specific target and the specification
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Slice<Action> findActionsByTarget(@NotNull String rsql, @NotEmpty String controllerId, @NotNull Pageable pageable);

    /**
     * Retrieves all {@link Action}s which are referring the given {@link Target}.
     *
     * @param controllerId the target to find actions for
     * @param pageable the pageable request to limit, sort the actions
     * @return a slice of actions found for a specific target
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Slice<Action> findActionsByTarget(@NotEmpty String controllerId, @NotNull Pageable pageable);

    /**
     * Retrieves all the {@link ActionStatus} entries of the given {@link Action}.
     *
     * @param actionId to be filtered on
     * @param pageable pagination parameter
     * @return the corresponding {@link Page} of {@link ActionStatus}
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<ActionStatus> findActionStatusByAction(long actionId, @NotNull Pageable pageable);

    /**
     * Retrieves all messages for an {@link ActionStatus}.<p/>
     *
     * @param actionStatusId the id of {@link ActionStatus} to retrieve the messages from
     * @param pageable the page request parameter for paging and sorting the result
     * @return a page of messages by a specific {@link ActionStatus} id
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<String> findMessagesByActionStatusId(long actionStatusId, @NotNull Pageable pageable);

    /**
     * Get the {@link Action} entity for given actionId with all lazy attributes (i.e. distributionSet, target, target.assignedDs).
     *
     * @param actionId to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Optional<Action> findActionWithDetails(long actionId);

    /**
     * Retrieves all active {@link Action}s of a specific target.
     *
     * @param controllerId the target associated with the actions
     * @param pageable the page request parameter for paging and sorting the result
     * @return a list of actions associated with the given target
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Page<Action> findActiveActionsByTarget(@NotEmpty String controllerId, @NotNull Pageable pageable);

    /**
     * Retrieves active {@link Action}s with highest weight that are assigned to a {@link Target}.
     *
     * @param controllerId identifies the target to retrieve the action from
     * @param maxActionCount max size of returned list
     * @return the action
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    List<Action> findActiveActionsWithHighestWeight(@NotEmpty String controllerId, int maxActionCount);

    /**
     * Force cancels given {@link Action} for given {@link Target}. Force canceling means that the action is marked as canceled on the SP server
     * and a cancel request is sent to the target. But however it's not tracked, if the targets handles the cancel request or not.
     *
     * @param actionId to be canceled
     * @return quite {@link Action}
     * @throws CancelActionNotAllowedException in case the given action is not active
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    Action forceQuitAction(long actionId);

    /**
     * Updates a {@link Action} and forces the {@link Action} if it's not already forced.
     *
     * @param actionId the ID of the action
     * @return the updated or the found {@link Action}
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    Action forceTargetAction(long actionId);

    /**
     * Deletes the current action by id.
     *
     * @param actionId - action id
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TARGET + "')")
    void deleteAction(long actionId);

    /**
     * Deletes actions matching the provided rsql filter
     *
     * @param rsql - rsql filter for actions to be deleted
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TARGET + "')")
    void deleteActionsByRsql(String rsql);

    /**
     * Deletes actions present in provided list of ids
     *
     * @param actionIds - list of action ids to be deleted
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TARGET + "')")
    void deleteActionsByIds(List<Long> actionIds);

    /**
     * Deletes actions in scope of the controllerId ONLY by list of action ids.
     *
     * @param controllerId - controllerId controllerId
     * @param actionsIds - list of action ids to be deleted
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TARGET + "')")
    void deleteTargetActionsByIds(final String controllerId, final List<Long> actionsIds);

    /**
     * Deletes controllerId actions and leaves the LAST N actions in the action history only.
     *
     * @param controllerId - controllerId controllerId
     * @param keepLast - number of actions to be left/kept (NOT deleted)
     */
    @PreAuthorize("hasAuthority('UPDATE_" + SpPermission.TARGET + "')")
    void deleteOldestTargetActions(final String controllerId, final int keepLast);

    /**
     * Sets the status of inactive scheduled {@link Action}s for the specified {@link Target}s to {@link Status#CANCELED}
     *
     * @param targetIds ids of the {@link Target}s the actions belong to
     */
    @PreAuthorize(HAS_UPDATE_TARGET_OR_CREATE_UPDATE_ROLLOUT)
    void cancelInactiveScheduledActionsForTargets(List<Long> targetIds);

    /**
     * Starts all scheduled actions of an RolloutGroup parent.
     *
     * @param rolloutId the rollout the actions belong to
     * @param distributionSetId to assign
     * @param rolloutGroupParentId the parent rollout group the actions should reference. null references the first group
     */
    @PreAuthorize(HAS_UPDATE_TARGET_OR_CREATE_UPDATE_ROLLOUT)
    void startScheduledActionsByRolloutGroupParent(long rolloutId, long distributionSetId, Long rolloutGroupParentId);

    /**
     * Handles the target assignments. Shall be part of same group
     *
     * @param rolloutGroupActions rollouts group actions part of a same group
     */
    @PreAuthorize(HAS_UPDATE_TARGET_OR_CREATE_UPDATE_ROLLOUT)
    void startScheduledActions(final List<Action> rolloutGroupActions);

    /**
     * Returns {@link DistributionSet} that is assigned to given {@link Target}.
     *
     * @param controllerId of target
     * @return assigned {@link DistributionSet}
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Optional<DistributionSet> findAssignedDistributionSet(@NotEmpty String controllerId);

    /**
     * Returns {@link DistributionSet} that is installed on given {@link Target}.
     *
     * @param controllerId of target
     * @return installed {@link DistributionSet}
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    Optional<DistributionSet> findInstalledDistributionSet(@NotEmpty String controllerId);

    /**
     * Deletes actions which match one of the given action status and which have not been modified since the given (absolute) time-stamp.
     * Used for obsolete actions cleanup.
     *
     * @param status Set of action status.
     * @param lastModified A time-stamp in milliseconds.
     * @return The number of action entries that were deleted.
     */
    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    int deleteActionsByStatusAndLastModifiedBefore(@NotNull Set<Action.Status> status, long lastModified);

    /**
     * Checks if there is an action for the device with the given controller ID that is in the {@link Action.Status#CANCELING} state.
     *
     * @param targetId of target
     * @return if actions in CANCELING state are present
     */
    @PreAuthorize(HAS_READ_REPOSITORY)
    boolean hasPendingCancellations(@NotNull Long targetId);

    /**
     * Cancels all actions that refer to a given distribution set. This method is called when a distribution set is invalidated.
     *
     * @param cancelationType defines if a force or soft cancel is executed
     * @param set the distribution set for that the actions should be canceled
     */
    @PreAuthorize(HAS_UPDATE_REPOSITORY)
    void cancelActionsForDistributionSet(final ActionCancellationType cancelationType, final DistributionSet set);

    @PreAuthorize(HAS_UPDATE_REPOSITORY + " or " + SpringEvalExpressions.IS_SYSTEM_CODE)
    void handleMaxAssignmentsExceeded(Long targetId, Long requested, AssignmentQuotaExceededException quotaExceededException);
}