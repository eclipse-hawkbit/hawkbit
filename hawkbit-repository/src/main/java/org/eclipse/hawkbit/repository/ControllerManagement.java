/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ToManyAttributeEntriesException;
import org.eclipse.hawkbit.repository.exception.ToManyStatusEntriesException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for all operations of the DDI API (with access permissions only
 * for the controller).
 *
 */
public interface ControllerManagement {

    String SERVER_MESSAGE_PREFIX = "Update Server: ";

    /**
     * Simple addition of a new {@link ActionStatus} entry to the {@link Action}
     * . No state changes.
     * 
     * @param statusMessage
     *            to add to the action
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    void addInformationalActionStatus(@NotNull ActionStatus statusMessage);

    /**
     * Adds an {@link ActionStatus} for a cancel {@link Action} including
     * potential state changes for the target and the {@link Action} itself.
     * 
     * @param actionStatus
     *            to be added
     * @return the persisted {@link Action}
     * 
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * 
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action addCancelActionStatus(@NotNull ActionStatus actionStatus);

    /**
     * Adds an {@link ActionStatus} entry for an update {@link Action} including
     * potential state changes for the target and the {@link Action} itself.
     *
     * @param actionStatus
     *            to be added
     * @return the updated {@link Action}
     *
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws ToManyStatusEntriesException
     *             if more than the allowed number of status entries are
     *             inserted
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action addUpdateActionStatus(@NotNull ActionStatus actionStatus);

    /**
     * Retrieves all {@link Action}s which are active and assigned to a
     * {@link Target}.
     *
     * @param target
     *            the target to retrieve the actions from
     * @return a list of actions assigned to given target which are active
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    List<Action> findActionByTargetAndActive(@NotNull Target target);

    /**
     * Get the {@link Action} entity for given actionId with all lazy
     * attributes.
     *
     * @param actionId
     *            to be id of the action
     * @return the corresponding {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action findActionWithDetails(@NotNull Long actionId);

    /**
     * register new target in the repository (plug-and-play).
     *
     * @param controllerId
     *            reference
     * @param address
     *            the client IP address of the target, might be {@code null}
     * @return target reference
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target findOrRegisterTargetIfItDoesNotexist(@NotEmpty String controllerId, URI address);

    /**
     * Retrieves all {@link SoftwareModule}s which are assigned to the given
     * {@link DistributionSet}.
     *
     * @param distributionSet
     *            the distribution set which should be assigned to the returned
     *            {@link SoftwareModule}s
     * @return a list of {@link SoftwareModule}s assigned to given
     *         {@code distributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    List<SoftwareModule> findSoftwareModulesByDistributionSet(@NotNull DistributionSet distributionSet);

    /**
     * Retrieves last {@link UpdateAction} for a download of an artifact of
     * given module and target.
     *
     * @param controllerId
     *            to look for
     * @param module
     *            that should be assigned to the target
     * @return last {@link UpdateAction} for given combination
     *
     * @throws EntityNotFoundException
     *             if action for given combination could not be found
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action getActionForDownloadByTargetAndSoftwareModule(@NotEmpty String controllerId, @NotNull SoftwareModule module);

    /**
     * @return current {@link TenantConfigurationKey#POLLING_TIME_INTERVAL}.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    String getPollingTime();

    /**
     * An direct access to the security token of an
     * {@link Target#getSecurityToken()} without authorization. This is
     * necessary to be able to access the security-token without any
     * security-context information because the security-token is used for
     * authentication.
     *
     * @param controllerId
     *            the ID of the controller to retrieve the security token for
     * @return the security context of the target, in case no target exists for
     *         the given controllerId {@code null} is returned
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    String getSecurityTokenByControllerId(@NotEmpty String controllerId);

    /**
     * Checks if a given target has currently or has even been assigned to the
     * given artifact through the action history list. This can e.g. indicate if
     * a target is allowed to download a given artifact because it has currently
     * assigned or had ever been assigned to the target and so it's visible to a
     * specific target e.g. for downloading.
     * 
     * @param controllerId
     *            the ID of the target to check
     * @param localArtifact
     *            the artifact to verify if the given target had even been
     *            assigned to
     * @return {@code true} if the given target has currently or had ever a
     *         relation to the given artifact through the action history,
     *         otherwise {@code false}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    boolean hasTargetArtifactAssigned(@NotNull String controllerId, @NotNull LocalArtifact localArtifact);

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param action
     *            to the handle status for
     * @param message
     *            for the status
     * @return the update action in case the status has been changed to
     *         {@link Status#RETRIEVED}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Action registerRetrieved(@NotNull Action action, String message);

    /**
     * Updates attributes of the controller.
     *
     * @param controllerId
     *            to update
     * @param attributes
     *            to insert
     *
     * @return updated {@link Target}
     *
     * @throws EntityNotFoundException
     *             if target that has to be updated could not be found
     * @throws ToManyAttributeEntriesException
     *             if maximum
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target updateControllerAttributes(@NotEmpty String controllerId, @NotNull Map<String, String> attributes);

    /**
     * Refreshes the time of the last time the controller has been connected to
     * the server.
     *
     * @param controllerId
     *            of the target to to update
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated target
     *
     * @throws EntityNotFoundException
     *             if target with given ID could not be found
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    Target updateLastTargetQuery(@NotEmpty String controllerId, URI address);

    /**
     * Refreshes the time of the last time the controller has been connected to
     * the server.
     *
     * @param target
     *            to update
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated target
     *
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    TargetInfo updateLastTargetQuery(@NotNull TargetInfo target, @NotNull URI address);

    /**
     * Update selective the target status of a given {@code target}.
     *
     * @param targetInfo
     *            the target to update the target status
     * @param status
     *            the status to be set of the target. Might be {@code null} if
     *            the target status should not be updated
     * @param lastTargetQuery
     *            the last target query to be set of the target. Might be
     *            {@code null} if the target lastTargetQuery should not be
     *            updated
     * @param address
     *            the client address of the target, might be {@code null}
     * @return the updated TargetInfo
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER)
    TargetInfo updateTargetStatus(@NotNull TargetInfo targetInfo, TargetUpdateStatus status, Long lastTargetQuery,
            URI address);

}
