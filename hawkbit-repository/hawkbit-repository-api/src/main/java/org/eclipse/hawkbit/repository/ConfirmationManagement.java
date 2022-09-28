/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;

import javax.validation.constraints.NotEmpty;
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
    Optional<AutoConfirmationStatus> getStatus(@NotEmpty String controllerId);

    /**
     * Auto confirm active actions for a specific controller ID having the
     * {@link Action.Status#WAIT_FOR_CONFIRMATION} status.
     * 
     * @param controllerId
     *            to confirm actions for
     * @return a list of confirmed actions
     */
    List<Action> autoConfirmActiveActions(@NotEmpty String controllerId);

    /**
     * Disable auto conformation for a specific controller id
     *
     * @param controllerId
     *            to disable auto confirmation for
     */
    void disableAutoConfirmation(@NotEmpty String controllerId);

}
