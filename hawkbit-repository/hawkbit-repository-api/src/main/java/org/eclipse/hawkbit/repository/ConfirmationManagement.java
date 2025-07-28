/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service layer for all confirmation related operations.
 */
public interface ConfirmationManagement {

    String CONFIRMATION_CODE_MSG_PREFIX = "Confirmation status code: %d";

    /**
     * Activate auto confirmation for a given controller ID. In case auto confirmation is active already, this method will fail with an exception.
     *
     * @param controllerId to activate the feature for
     * @param initiator who initiated this operation. If 'null' we will take the current user from {@link TenantAware#getCurrentUsername()}
     * @param remark optional field to set a remark
     * @return the persisted {@link AutoConfirmationStatus}
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    AutoConfirmationStatus activateAutoConfirmation(@NotEmpty String controllerId, final String initiator, final String remark);

    /**
     * Confirm a given action to put it from {@link Action.Status#WAIT_FOR_CONFIRMATION} to {@link Action.Status#RUNNING} state.
     *
     * @param actionId mandatory to know which action to confirm
     * @param code optional value to specify a code for the created action status
     * @param messages optional value to specify message for the created action status
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    Action confirmAction(long actionId, Integer code, Collection<String> messages);

    /**
     * Deny a given action and leave it in {@link Action.Status#WAIT_FOR_CONFIRMATION} state.
     *
     * @param actionId mandatory to know which action to deny
     * @param code optional value to specify a code for the created action status
     * @param messages optional value to specify message for the created action status
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    Action denyAction(long actionId, Integer code, Collection<String> messages);

    /**
     * Deactivate auto confirmation for a specific controller id
     *
     * @param controllerId to disable auto confirmation for
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER_OR_HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    void deactivateAutoConfirmation(@NotEmpty String controllerId);

    /**
     * Get the current state of auto-confirmation for a given controllerId
     *
     * @param controllerId to check the state for
     * @return instance of {@link AutoConfirmationStatus} wrapped in an {@link Optional}. Present if active and empty if disabled.
     */
    @PreAuthorize(SpringEvalExpressions.IS_CONTROLLER + SpringEvalExpressions.HAS_AUTH_OR +
            SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<AutoConfirmationStatus> getStatus(@NotEmpty String controllerId);

    /**
     * Find active actions in the {@link Action.Status#WAIT_FOR_CONFIRMATION} state for a specific target with a specified controllerId.
     *
     * @param controllerId of the target to check
     * @return a list of {@link Action}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Action> findActiveActionsWaitingConfirmation(@NotEmpty String controllerId);
}