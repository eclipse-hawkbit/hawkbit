/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import static org.eclipse.hawkbit.repository.model.Action.ActionType;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.DOWNLOAD_ONLY;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.FORCED;
import static org.eclipse.hawkbit.repository.model.Action.ActionType.SOFT;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public interface AutoAssignment extends NamedEntity {

    /**
     * Maximum length of author name.
     */
    int APPROVED_BY_MAX_SIZE = 64;

    /**
     * Maximum length on comment regarding approval decision.
     */
    int APPROVAL_REMARK_MAX_SIZE = 255;

    /**
     * Maximum length of access control context.
     */
    int ACCESS_CONTROL_CONTEXT_MAX_SIZE = 32768;

    /**
     * Allowed values for action type
     */
    Set<ActionType> ALLOWED_ACTION_TYPES = Collections
            .unmodifiableSet(EnumSet.of(FORCED, SOFT, DOWNLOAD_ONLY));

    /**
     * @return RSQL query
     */
    String getTargetFilterQuery();

    /**
     * @return the {@link DistributionSet} if given.
     */
    DistributionSet getDistributionSet();

    /**
     * @return the {@link ActionType} if given.
     */
    ActionType getActionType();

    /**
     * @return Timestamp when the auto assignment should be started automatically. Can be null.
     */
    Long getStartAt();

    /**
     * @return status of the auto assignment
     */
    AutoAssignStatus getStatus();

    /**
     * @return user that approved or denied the auto assignment
     */
    String getApprovalDecidedBy();

    /**
     * @return additional note on approval/denial decision.
     */
    String getApprovalRemark();

    /**
     * @return the weight of the {@link Action}s created during an auto assignment.
     */
    Optional<Integer> getWeight();

    /**
     * @return if confirmation is required for configured auto assignment
     *             (considered with confirmation flow active)
     */
    boolean isConfirmationRequired();

    /**
     * State machine for an auto assignment
     */
    enum AutoAssignStatus {

        /**
         * Auto assignment needs to be approved
         */
        WAITING_FOR_APPROVAL,

        /**
         * Auto assignment is denied. Cannot be started
         */
        APPROVAL_DENIED,

        /**
         * Auto assignment is ready to start
         */
        READY,

        /**
         * Auto assignment has been paused
         */
        PAUSED,

        /**
         * Auto assignment is running
         */
        RUNNING
    }

    enum AutoAssignApprovalDecision {

        APPROVED,

        DENIED
    }
}
