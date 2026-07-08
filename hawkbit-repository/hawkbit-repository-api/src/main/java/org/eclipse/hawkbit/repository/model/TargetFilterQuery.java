/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * Managed filter entity.
 * <p/>
 * Supported operators.
 * <ul>
 * <li>{@code Equal to : ==}</li>
 * <li>{@code Not equal to : !=}</li>
 * <li>{@code Less than : =lt= or <}</li>
 * <li>{@code Less than or equal to : =le= or <=}</li>
 * <li>{@code Greater than operator : =gt= or >}</li>
 * <li>{@code Greater than or equal to : =ge= or >=}</li>
 * </ul>
 * Examples of RSQL expressions in both FIQL-like and alternative notation:
 * <ul>
 * <li>{@code version==2.0.0}</li>
 * <li>{@code name==targetId1;description==plugAndPlay}</li>
 * <li>{@code name==targetId1 and description==plugAndPlay}</li>
 * <li>{@code name==targetId1;description==plugAndPlay}</li>
 * <li>{@code name==targetId1 and description==plugAndPlay}</li>
 * <li>{@code name==targetId1,description==plugAndPlay,updateStatus==UNKNOWN}</li>
 * <li>{@code name==targetId1 or description==plugAndPlay or updateStatus==UNKNOWN}</li>
 * </ul>
 */
public interface TargetFilterQuery extends TenantAwareBaseEntity {

    /**
     * Maximum length of query filter string.
     */
    int QUERY_MAX_SIZE = 1024;

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
     * Allowed values for auto-assign action type
     */
    Set<ActionType> ALLOWED_AUTO_ASSIGN_ACTION_TYPES = Collections
            .unmodifiableSet(EnumSet.of(ActionType.FORCED, ActionType.SOFT, ActionType.DOWNLOAD_ONLY));

    /**
     * @return name of the {@link TargetFilterQuery}.
     */
    String getName();

    /**
     * @return RSQL query
     */
    String getQuery();

    /**
     * @return the auto assign {@link DistributionSet} if given.
     */
    DistributionSet getAutoAssignDistributionSet();

    /**
     * @return the auto assign {@link ActionType} if given.
     */
    ActionType getAutoAssignActionType();

    /**
     * @return Timestamp when the auto assignment should be started automatically. Can be null.
     */
    Long getStartAt();

    /**
     * @return status of the auto assignment
     */
    AutoAssignStatus getAutoAssignStatus();

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
    Optional<Integer> getAutoAssignWeight();

    /**
     * @return the user that triggered the auto assignment
     */
    String getAutoAssignInitiatedBy();

    /**
     * @return if confirmation is required for configured auto assignment
     *         (considered with confirmation flow active)
     */
    boolean isConfirmationRequired();

    /**
     * Defining a serialized access control context which needs to be set when
     * performing the auto-assignment by the scheduler
     */
    Optional<String> getAccessControlContext();

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
}
