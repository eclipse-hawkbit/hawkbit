/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for all confirmation related operations.
 */
public interface ConfirmationManagement {

    /**
     * Find active actions in the {@link Action.Status#WAIT_FOR_CONFIRMATION} state
     * for a specific target with a specified controllerId.
     * 
     * @param controllerId
     *            of the target to check
     * @return a list of {@link Action}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Action> findActiveActionsWaitingConfirmation(@NotEmpty String controllerId);

    /**
     * Activate auto confirmation for a given controller ID. In case auto
     * confirmation is active already, this method will fail with an exception.
     *
     * @param controllerId
     *            to activate the feature for
     * @param initiator
     *            who initiated this operation. If 'null' we will take the current
     *            user from {@link TenantAware#getCurrentUsername()}
     * @param remark
     *            optional field to set a remark
     * @return the persisted {@link AutoConfirmationStatus}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    AutoConfirmationStatus activateAutoConfirmation(@NotEmpty String controllerId, final String initiator,
            final String remark);

    /**
     * Get the current state of auto-confirmation for a given controllerId
     *
     * @param controllerId
     *            to check the state for
     * @return instance of {@link AutoConfirmationStatus} wrapped in an
     *         {@link Optional}. Present if active and empty if disabled.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<AutoConfirmationStatus> getStatus(@NotEmpty String controllerId);

    /**
     * Auto confirm active actions for a specific controller ID having the
     * {@link Action.Status#WAIT_FOR_CONFIRMATION} status.
     * 
     * @param controllerId
     *            to confirm actions for
     * @return a list of confirmed actions
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    List<Action> autoConfirmActiveActions(@NotEmpty String controllerId);

    /**
     * Confirm a given action to put it from
     * {@link Action.Status#WAIT_FOR_CONFIRMATION} to {@link Action.Status#RUNNING}
     * state.
     * 
     * @param actionId
     *            mandatory to know which action to confirm
     * @param code
     *            optional value to specify a code for the created action status
     * @param messages
     *            optional value to specify message for the created action status
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    Action confirmAction(long actionId, Integer code, Collection<String> messages);

    /**
     * Deny a given action and leave it in
     * {@link Action.Status#WAIT_FOR_CONFIRMATION} state.
     *
     * @param actionId
     *            mandatory to know which action to deny
     * @param code
     *            optional value to specify a code for the created action status
     * @param messages
     *            optional value to specify message for the created action status
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    Action denyAction(long actionId, Integer code, Collection<String> messages);

    /**
     * Deactivate auto confirmation for a specific controller id
     *
     * @param controllerId
     *            to disable auto confirmation for
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    void deactivateAutoConfirmation(@NotEmpty String controllerId);

}
