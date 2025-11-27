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

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.tenancy.configuration.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service layer for all operations of the DDI API (with access permissions only for the controller).
 */
public interface ControllerManagement {

    /**
     * Adds an {@link ActionStatus} for a cancel {@link Action} including potential state changes for the target and the {@link Action} itself.
     *
     * @param create to be added
     * @return the updated {@link Action}
     * @throws EntityAlreadyExistsException if a given entity already exists
     * @throws AssignmentQuotaExceededException if more than the allowed number of status entries or messages per entry are inserted
     * @throws EntityNotFoundException if given action does not exist
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link ActionStatusCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action addCancelActionStatus(@NotNull @Valid ActionStatusCreate create);

    /**
     * Retrieves assigned {@link SoftwareModule} of a target.
     *
     * @param moduleId of the {@link SoftwareModule}
     * @return {@link SoftwareModule} identified by ID
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    SoftwareModule getSoftwareModule(long moduleId);

    /**
     * Retrieves software module metadata where isTargetVisible.
     *
     * @param moduleId of the {@link SoftwareModule}
     * @return the map of software module id to software module metadata with maximum size of {@link RepositoryConstants#MAX_META_DATA_COUNT}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Map<Long, Map<String, String>> findTargetVisibleMetaDataBySoftwareModuleId(@NotNull Collection<Long> moduleId);

    /**
     * Simple addition of a new {@link ActionStatus} entry to the {@link Action}. No state changes.
     *
     * @param create to add to the action
     * @return created {@link ActionStatus} entity
     * @throws AssignmentQuotaExceededException if more than the allowed number of status entries or messages per entry are inserted
     * @throws EntityNotFoundException if given action does not exist
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link ActionStatusCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    ActionStatus addInformationalActionStatus(@NotNull @Valid ActionStatusCreate create);

    /**
     * Adds an {@link ActionStatus} entry for an update {@link Action} including potential state changes for the target and the {@link Action}
     * itself.
     *
     * @param create to be added
     * @return the updated {@link Action}
     * @throws EntityAlreadyExistsException if a given entity already exists
     * @throws AssignmentQuotaExceededException if more than the allowed number of status entries or messages per entry are inserted
     * @throws EntityNotFoundException if action status not exist
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link ActionStatusCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action addUpdateActionStatus(@NotNull @Valid ActionStatusCreate create);

    /**
     * Retrieves active {@link Action} with the highest priority that is assigned to a {@link Target}.
     * <p/>
     * For performance reasons this method does not throw {@link EntityNotFoundException} in case target with given controllerId
     * does not exist but will return an {@link Optional#empty()} instead.
     *
     * @param controllerId identifies the target to retrieve the action from
     * @return the action
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Optional<Action> findActiveActionWithHighestWeight(@NotEmpty String controllerId);

    /**
     * Retrieves active {@link Action}s with the highest weight that are assigned to a {@link Target}.
     *
     * @param controllerId identifies the target to retrieve the action from
     * @param maxActionCount max size of returned list
     * @return the action
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    List<Action> findActiveActionsWithHighestWeight(@NotEmpty String controllerId, int maxActionCount);

    /**
     * Get the {@link Action} entity for given actionId with all lazy attributes.
     *
     * @param actionId to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Optional<Action> findActionWithDetails(long actionId);

    /**
     * Retrieves all the {@link ActionStatus} entries of the given {@link Action}.
     *
     * @param actionId to be filtered on
     * @param pageable pagination parameter
     * @return the corresponding {@link Page} of {@link ActionStatus}
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Page<ActionStatus> findActionStatusByAction(long actionId, @NotNull Pageable pageable);

    /**
     * Register new target in the repository (plug-and-play) and in case it already exists updates {@link Target#getAddress()} and
     * {@link Target#getLastTargetQuery()} and switches if {@link TargetUpdateStatus#UNKNOWN} to {@link TargetUpdateStatus#REGISTERED}.
     *
     * @param controllerId reference
     * @param address the client IP address of the target, might be {@code null}
     * @return target reference
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target findOrRegisterTargetIfItDoesNotExist(@NotEmpty String controllerId, @NotNull URI address);

    /**
     * Register new target in the repository (plug-and-play) and in case it
     * already exists updates {@link Target#getAddress()} and
     * {@link Target#getLastTargetQuery()} and {@link Target#getName()}
     * and {@link Target#getTargetType()} and
     * switches if {@link TargetUpdateStatus#UNKNOWN} to
     * {@link TargetUpdateStatus#REGISTERED}.
     *
     * @param controllerId reference
     * @param address the client IP address of the target, might be {@code null}
     * @param name the name of the target
     * @param type the target type name of the target
     * @return target reference
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target findOrRegisterTargetIfItDoesNotExist(@NotEmpty String controllerId, @NotNull URI address, String name, String type);

    /**
     * Retrieves last {@link Action} for a download of an artifact of given module and target if exists and is not canceled.
     *
     * @param controllerId to look for
     * @param moduleId of the {@link SoftwareModule} that should be assigned to the target
     * @return last {@link Action} for given combination
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action getActionForDownloadByTargetAndSoftwareModule(@NotEmpty String controllerId, long moduleId);

    /**
     * Returns configured polling interval at which the controller polls hawkBit server.
     *
     * @param target {@link Target} for which polling time is calculated (it could be overridden for a specific targets).
     * @return current {@link TenantConfigurationKey#POLLING_TIME}.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    String getPollingTime(Target target);

    /**
     * Returns polling time based on the maintenance window for an action. Server will reduce the polling interval as the start time for
     * maintenance window approaches, so that at least these many attempts are made between current polling until start of maintenance window.
     * Poll time keeps reducing with MinPollingTime as lower limit {@link ControllerPollProperties#getMinPollingTime()}. After the start
     * of maintenance window, it resets to default {@link TenantConfigurationKey#POLLING_TIME}.
     *
     * @param target {@link Target} for which polling time is calculated
     * @param action {@link Action} for which polling time is calculated based on it having maintenance window or not
     * @return current {@link TenantConfigurationKey#POLLING_TIME}.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    String getPollingTimeForAction(Target target, Action action);

    /**
     * Checks if a given target has currently or has even been assigned to the given artifact through the action history list. This can e.g.
     * indicate if a target is allowed to download a given artifact because it has currently assigned or had ever been assigned to the target
     * and so it's visible to a specific target e.g. for downloading.
     *
     * @param controllerId the ID of the target to check
     * @param sha1Hash of the artifact to verify if the given target had even been assigned to
     * @return {@code true} if the given target has currently or had ever a relation to the given artifact through the action history,
     *         otherwise {@code false}
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    boolean hasTargetArtifactAssigned(@NotEmpty String controllerId, @NotEmpty String sha1Hash);

    /**
     * Checks if a given target has currently or has even been assigned to the given artifact through the action history list. This can e.g.
     * indicate if a target is allowed to download a given artifact because it has currently assigned or had ever been assigned to the target
     * and so it's visible to a specific target e.g. for downloading.
     *
     * @param targetId the ID of the target to check
     * @param sha1Hash of the artifact to verify if the given target had even been assigned to
     * @return {@code true} if the given target has currently or had ever a relation to the given artifact through the action history,
     *         otherwise {@code false}
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    boolean hasTargetArtifactAssigned(long targetId, @NotEmpty String sha1Hash);

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if it does not exist yet.
     *
     * @param actionId to the handle status for
     * @param message for the status
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    void registerRetrieved(long actionId, String message);

    /**
     * Updates attributes of the controller according to the given {@link UpdateMode}.
     *
     * @param controllerId to update
     * @param attributes to insert
     * @param mode the update mode or <code>null</code>
     * @return updated {@link Target}
     * @throws EntityNotFoundException if target that has to be updated could not be found
     * @throws AssignmentQuotaExceededException if maximum number of attributes per target is exceeded
     * @throws InvalidTargetAttributeException if attributes violate constraints
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target updateControllerAttributes(@NotEmpty String controllerId, @NotNull Map<String, String> attributes, UpdateMode mode);

    /**
     * Finds {@link Target} based on given controller ID returns found Target without details
     *
     * @param controllerId to look for.
     * @return {@link Target} or {@code null} if it does not exist
     * @see Target#getControllerId()
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER + " or " + SpringEvalExpressions.IS_SYSTEM_CODE)
    Optional<Target> findByControllerId(@NotEmpty String controllerId);

    /**
     * Finds {@link Target} based on given ID returns found Target without details
     *
     * @param targetId to look for.
     * @return {@link Target} or {@code null} if it does not exist
     * @see Target#getId()
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER + " or " + SpringEvalExpressions.IS_SYSTEM_CODE)
    Optional<Target> find(long targetId);

    /**
     * Retrieves the specified number of messages from action history of the given {@link Action} based on messageCount. Regardless of the
     * value of messageCount, in order to restrict resource utilisation by controllers, maximum number of messages that are retrieved from
     * database is limited by {@link RepositoryConstants#MAX_ACTION_HISTORY_MSG_COUNT}. messageCount less than zero, retrieves the maximum
     * allowed number of action status messages from history; messageCount equal zero, does not retrieve any message; and messageCount larger
     * than zero, retrieves the specified number of messages, limited by maximum allowed number. A controller sends the feedback for an
     * {@link ActionStatus} as a list of messages; while returning the messages, even though the messages from multiple {@link ActionStatus}
     * are retrieved in descending order by the reported time ({@link ActionStatus#getTimestamp()}), i.e. latest ActionStatus first, the
     * sub-ordering of messages from within single {@link ActionStatus} is unspecified.
     *
     * @param actionId to be filtered on
     * @param messageCount is the number of messages to return from history
     * @return action history.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    List<String> getActionHistoryMessages(long actionId, int messageCount);

    /**
     * Cancels given {@link Action} for this {@link Target}. The method will immediately add a {@link Status#CANCELED}.
     * However, it might be possible that the controller will continue to work on the cancellation. The controller needs to acknowledge or
     * reject the cancellation using DdiRootController#postCancelActionFeedback
     *
     * @param action to be canceled
     * @return canceled {@link Action}
     * @throws CancelActionNotAllowedException in case the given action is not active or is already canceled
     * @throws EntityNotFoundException if action with given actionId does not exist.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action cancelAction(Action action);

    /**
     * Updates given {@link Action} with its external id.
     *
     * @param actionId to be updated
     * @param externalRef of the action
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    void updateActionExternalRef(long actionId, @NotEmpty String externalRef);

    /**
     * Delete a single target.
     *
     * @param controllerId of the target to delete
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    void deleteExistingTarget(@NotEmpty String controllerId);

    /**
     * Finds an {@link Action} based on the target that it's assigned to
     *
     * @param target the target the action is assigned to
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Optional<Action> findInstalledActionByTarget(@NotNull Target target);

    /**
     * Activate auto confirmation for a given controllerId
     *
     * @param controllerId to activate auto-confirmation on
     * @param initiator can be set optionally (fallback is the current acting security user)
     * @param remark (optional) remark
     * @return the persisted {@link AutoConfirmationStatus}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    AutoConfirmationStatus activateAutoConfirmation(@NotEmpty String controllerId, String initiator, String remark);

    /**
     * Deactivate auto confirmation for a given controllerId
     *
     * @param controllerId to deactivate auto-confirmation on
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    void deactivateAutoConfirmation(@NotEmpty String controllerId);

    /**
     * Updates distributionSet installed version (experimental)
     *
     * @param distributionName installed
     * @param version installed
     * @return updated {@link Target}
     * @throws EntityNotFoundException if target that has to be updated could not be found
     * @throws java.util.NoSuchElementException if DistributionSetAssignmentResult list is empty
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    boolean updateOfflineAssignedVersion(@NotEmpty String controllerId, String distributionName, String version);
}