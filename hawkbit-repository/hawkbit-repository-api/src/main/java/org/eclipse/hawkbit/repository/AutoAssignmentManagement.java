/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.eclipse.hawkbit.auth.SpPermission.APPROVE_AUTO_ASSIGNMENT;
import static org.eclipse.hawkbit.auth.SpPermission.HANDLE_AUTO_ASSIGNMENT;

import java.util.Optional;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AutoAssignmentIllegalStateException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Auto assignment management to control auto assignments e.g. like creating, starting, resuming and pausing rollouts.
 * This service secures all the functionality based on the {@link PreAuthorize} annotation on methods.
 */
public interface AutoAssignmentManagement<T extends AutoAssignment>
        extends RepositoryManagement<T, AutoAssignmentManagement.Create, AutoAssignmentManagement.Update> {

    String HAS_AUTO_ASSIGNMENT_APPROVE = "hasPermission(#root, '" + APPROVE_AUTO_ASSIGNMENT + "')";
    String HAS_AUTO_ASSIGNMENT_HANDLE = "hasPermission(#root, '" + HANDLE_AUTO_ASSIGNMENT + "')";

    @Override
    default String permissionGroup() {
        return SpPermission.AUTO_ASSIGNMENT;
    }

    /**
     * Retrieves all {@link AutoAssignment}s which match the given distribution set and RSQL filter.
     *
     * @param setId the distribution set id
     * @param rsql RSQL filter
     * @param pageable pagination parameter
     * @return the page with the found {@link AutoAssignment}s
     * @throws EntityNotFoundException if DS with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY + " and " + "hasAuthority('READ_" + SpPermission.DISTRIBUTION_SET + "')")
    Page<AutoAssignment> findByDSAndRsql(long setId, String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves all {@link AutoAssignment}s with the status {@link AutoAssignment.AutoAssignStatus#RUNNING}
     * or {@link AutoAssignment.AutoAssignStatus#READY}.
     *
     * @param pageable pagination information
     * @return the page with the found {@link AutoAssignment}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Slice<AutoAssignment> getActiveAutoAssignments(@NotNull Pageable pageable);

    /**
     * Retrieves all {@link AutoAssignment}s - regardless of their
     * auto-assign status - and match the given RSQL filter.
     *
     * @param rsql RSQL filter, may be {@code null} or empty to match all auto assignments
     * @param pageable pagination information
     * @return the page with the found {@link AutoAssignment}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<AutoAssignment> findAutoAssignmentByRsql(String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves the {@link AutoAssignment} with the given name. Names are unique per tenant, so at most one is returned.
     *
     * @param name the name of the auto assignment
     * @return the matching {@link AutoAssignment} or an empty {@link Optional} if none exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<AutoAssignment> findByName(@NotNull String name);

    /**
     * Removes the given {@link DistributionSet} from all auto assignments.
     *
     * @param setId the {@link DistributionSet} to be removed from auto
     *            assignments.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void cancelAutoAssignmentForDistributionSet(long setId);

    /**
     * Approves or denies a created auto assignment in state {@link AutoAssignment.AutoAssignStatus#WAITING_FOR_APPROVAL}. If the auto
     * assignment is approved,
     * it switches state to {@link AutoAssignment.AutoAssignStatus#READY}, otherwise it switches to state
     * {@link AutoAssignment.AutoAssignStatus#APPROVAL_DENIED}
     *
     * @param autoAssignmentId the auto assignment to be approved or denied.
     * @param decision decision whether an auto assignment is approved or denied.
     * @return target filter query of the approved/denied auto assignment
     * @throws EntityNotFoundException if target filter query with given ID does not exist
     * @throws AutoAssignmentIllegalStateException if given auto assignment is not in
     *             {@link AutoAssignment.AutoAssignStatus#WAITING_FOR_APPROVAL}. Only auto assignments
     *             waiting for approval can be acted upon.
     */
    @PreAuthorize(HAS_AUTO_ASSIGNMENT_APPROVE)
    AutoAssignment approveOrDeny(long autoAssignmentId, AutoAssignment.AutoAssignApprovalDecision decision);

    /**
     * Approves or denies a created auto assignment in state {@link AutoAssignment.AutoAssignStatus#WAITING_FOR_APPROVAL}. If the auto
     * assignment is approved,
     * it switches state to {@link AutoAssignment.AutoAssignStatus#READY}, otherwise it switches to state
     * {@link AutoAssignment.AutoAssignStatus#APPROVAL_DENIED}
     *
     * @param autoAssignmentId the auto assignment to be approved or denied.
     * @param decision decision whether an auto assignment is approved or denied.
     * @param remark user remark on approve / deny decision
     * @return target filter query of the approved/denied auto assignment
     * @throws EntityNotFoundException if target filter query with given ID does not exist
     * @throws AutoAssignmentIllegalStateException if given auto assignment is not in
     *             {@link AutoAssignment.AutoAssignStatus#WAITING_FOR_APPROVAL}. Only auto assignments
     *             waiting for approval can be acted upon.
     */
    @PreAuthorize(HAS_AUTO_ASSIGNMENT_APPROVE)
    AutoAssignment approveOrDeny(long autoAssignmentId, AutoAssignment.AutoAssignApprovalDecision decision, String remark);

    /**
     * Starts an auto assignment which is in {@link AutoAssignment.AutoAssignStatus#READY} state. The auto assignment is set into the
     * {@link AutoAssignment.AutoAssignStatus#RUNNING} state, so that it is picked up by the scheduler.
     *
     * @param autoAssignmentId the auto assignment to be started
     * @return started target filter query
     * @throws EntityNotFoundException if target filter query with given ID does not exist
     * @throws AutoAssignmentIllegalStateException if given auto assignment is not in {@link AutoAssignment.AutoAssignStatus#READY}. Only
     *             ready auto assignments can be started.
     */
    @PreAuthorize(HAS_AUTO_ASSIGNMENT_HANDLE)
    AutoAssignment start(final long autoAssignmentId);

    /**
     * Pauses an auto assignment which is currently running. The auto assignment switches to {@link AutoAssignment.AutoAssignStatus#PAUSED}
     * state and is no longer picked up by the scheduler until it is resumed via {@link #resume(long)}.
     *
     * @param autoAssignmentId the auto assignment to be paused
     * @return paused target filter query
     * @throws EntityNotFoundException if target filter query with given ID does not exist
     * @throws AutoAssignmentIllegalStateException if given auto assignment is not in {@link AutoAssignment.AutoAssignStatus#RUNNING}. Only
     *             running auto assignments can be paused.
     */
    @PreAuthorize(HAS_AUTO_ASSIGNMENT_HANDLE)
    AutoAssignment pause(final long autoAssignmentId);

    /**
     * Resumes a paused auto assignment. The auto assignment switches back to {@link AutoAssignment.AutoAssignStatus#RUNNING} state which
     * is then picked up again by the scheduler.
     *
     * @param autoAssignmentId the auto assignment to be resumed
     * @return resumed target filter query
     * @throws EntityNotFoundException if target filter query with given ID does not exist
     * @throws AutoAssignmentIllegalStateException if given auto assignment is not in {@link AutoAssignment.AutoAssignStatus#PAUSED}. Only
     *             paused auto assignments can be resumed.
     */
    @PreAuthorize(HAS_AUTO_ASSIGNMENT_HANDLE)
    AutoAssignment resume(final long autoAssignmentId);

    /**
     * Builder to update the {@link AutoAssignment}. Defines all fields that can be updated.
     */
    @SuperBuilder
    @Getter
    @EqualsAndHashCode
    @ToString
    final class Update implements Identifiable<Long> {

        @NotNull
        private Long id;
        @Setter
        private String name;
        @Setter
        private String description;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode
    @ToString
    class Create {

        private String name;
        private String description;
        @NotEmpty
        private String targetFilterQuery;
        @NotNull
        @Setter
        private DistributionSet distributionSet;
        private Long startAt;
        private Action.ActionType actionType;
        @Setter
        @Min(Action.WEIGHT_MIN)
        @Max(Action.WEIGHT_MAX)
        private Integer weight;
        private boolean confirmationRequired;
    }
}