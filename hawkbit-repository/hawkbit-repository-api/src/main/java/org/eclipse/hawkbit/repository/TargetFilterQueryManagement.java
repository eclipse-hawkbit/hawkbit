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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetFilterQuery}s.
 */
public interface TargetFilterQueryManagement<T extends TargetFilterQuery>
        extends RepositoryManagement<T, TargetFilterQueryManagement.Create, TargetFilterQueryManagement.Update> {

    @Override
    default String permissionGroup() {
        return SpPermission.TARGET;
    }

    /**
     * Verifies the provided filter syntax.
     *
     * @param query to verify
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    void verifyTargetFilterQuerySyntax(@NotNull String query);

    /**
     * Counts all target filters that have a given auto assign distribution set
     * assigned.
     * <p/>
     * No access control applied
     *
     * @param autoAssignDistributionSetId the id of the distribution set
     * @return the count
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    long countByAutoAssignDistributionSetId(long autoAssignDistributionSetId);

    /**
     * Retrieves all {@link TargetFilterQuery}s which match the given auto-assign distribution set and RSQL filter.
     *
     * @param setId the auto assign distribution set
     * @param rsql RSQL filter
     * @param pageable pagination parameter
     * @return the page with the found {@link TargetFilterQuery}s
     * @throws EntityNotFoundException if DS with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY + " and " + "hasAuthority('READ_" + SpPermission.DISTRIBUTION_SET + "')")
    Page<TargetFilterQuery> findByAutoAssignDSAndRsql(long setId, String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves all {@link TargetFilterQuery}s with an auto-assign distribution set.
     *
     * @param pageable pagination information
     * @return the page with the found {@link TargetFilterQuery}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Slice<TargetFilterQuery> findWithAutoAssignDS(@NotNull Pageable pageable);

    /**
     * Updates the auto assign settings of an {@link TargetFilterQuery}.
     *
     * @param autoAssignDistributionSetUpdate the new auto assignment
     * @return the updated {@link TargetFilterQuery}
     * @throws EntityNotFoundException if either {@link TargetFilterQuery} and/or autoAssignDs are
     *         provided but not found
     * @throws AssignmentQuotaExceededException if the query that is already associated with this filter
     *         query addresses too many targets (auto-assignments only)
     * @throws InvalidAutoAssignActionTypeException if the provided auto-assign {@link ActionType} is not valid
     *         (neither FORCED, nor SOFT)
     * @throws IncompleteDistributionSetException if the provided auto-assign {@link DistributionSet} is
     *         incomplete
     * @throws InvalidDistributionSetException if the provided auto-assign {@link DistributionSet} is
     *         invalidated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    TargetFilterQuery updateAutoAssignDS(@NotNull @Valid AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate);

    /**
     * Removes the given {@link DistributionSet} from all auto assignments.
     *
     * @param setId the {@link DistributionSet} to be removed from auto
     *         assignments.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void cancelAutoAssignmentForDistributionSet(long setId);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        private DistributionSet autoAssignDistributionSet;
        private ActionType autoAssignActionType;
        @Setter
        @Min(Action.WEIGHT_MIN)
        @Max(Action.WEIGHT_MAX)
        private Integer autoAssignWeight;
        private boolean confirmationRequired;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = DistributionSetTagManagement.Create.class)
        private String name;

        @ValidString
        @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
        private String query;
    }

    /**
     * Builder to update the auto assign {@link DistributionSet} of a
     * {@link TargetFilterQuery} entry. Defines all fields that can be updated.
     */
    @Data
    @Accessors(fluent = true)
    @EqualsAndHashCode
    @ToString
    class AutoAssignDistributionSetUpdate {

        private final long targetFilterId;
        private Long dsId;
        private ActionType actionType;

        @Min(Action.WEIGHT_MIN)
        @Max(Action.WEIGHT_MAX)
        private Integer weight;

        private Boolean confirmationRequired;

        /**
         * Constructor
         *
         * @param targetFilterId ID of {@link TargetFilterQuery} to update
         */
        public AutoAssignDistributionSetUpdate(final long targetFilterId) {
            this.targetFilterId = targetFilterId;
        }

        /**
         * Specify {@link DistributionSet}
         *
         * @param dsId ID of the {@link DistributionSet}
         * @return updated builder instance
         */
        public AutoAssignDistributionSetUpdate ds(final Long dsId) {
            this.dsId = dsId;
            return this;
        }

        /**
         * Specify {@link DistributionSet}
         *
         * @param actionType {@link ActionType} used for the auto assignment
         * @return updated builder instance
         */
        public AutoAssignDistributionSetUpdate actionType(final ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        /**
         * Specify weight of resulting {@link Action}
         *
         * @param weight weight used for the auto assignment
         * @return updated builder instance
         */
        public AutoAssignDistributionSetUpdate weight(final Integer weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Specify initial confirmation state of resulting {@link Action}
         *
         * @param confirmationRequired if confirmation is required for this auto assignment (considered
         *         with confirmation flow active)
         * @return updated builder instance
         */
        public AutoAssignDistributionSetUpdate confirmationRequired(final boolean confirmationRequired) {
            this.confirmationRequired = confirmationRequired;
            return this;
        }
    }
}