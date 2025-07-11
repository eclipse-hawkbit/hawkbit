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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link Target}s.
 */
public interface TargetManagement {

    String DETAILS_BASE = "base";
    String DETAILS_AUTO_CONFIRMATION_STATUS = "autoConfirmationStatus";
    String DETAILS_TAGS = "tags";
    String DETAILS_ACTIONS = "actions";

    /**
     * Counts number of targets with the given distribution set assigned.
     *
     * @param distributionSetId to search for
     * @return number of found {@link Target}s.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    long countByAssignedDistributionSet(long distributionSetId);

    /**
     * Count {@link Target}s for all the given filter parameters.
     *
     * @param filterParams the filters to apply; only filters are enabled that have non-null
     *         value; filters are AND-gated
     * @return the found number {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByFilters(@NotNull final FilterParams filterParams);

    /**
     * Get the count of targets with the given distribution set id.
     *
     * @param distributionSetId to search for
     * @return number of found {@link Target}s.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    long countByInstalledDistributionSet(long distributionSetId);

    /**
     * Checks if there is already a {@link Target} that has the given distribution
     * set Id assigned or installed.
     *
     * @param distributionSetId to search for
     * @return <code>true</code> if a {@link Target} exists.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    boolean existsByInstalledOrAssignedDistributionSet(long distributionSetId);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query.
     *
     * @param rsql filter definition in RSQL syntax
     * @return the found number of {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsql(@NotEmpty String rsql);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query with UPDATE permission.
     *
     * @param rsql filter definition in RSQL syntax
     * @return the found number of {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndUpdatable(@NotEmpty String rsql);

    /**
     * Count all targets for given {@link TargetFilterQuery} and that are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param rsql filter definition in RSQL syntax
     * @param distributionSetIdTypeId ID of the {@link DistributionSetType} the targets need to be
     *         compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndCompatible(@NotEmpty String rsql, @NotNull Long distributionSetIdTypeId);

    /**
     * Count all targets for given {@link TargetFilterQuery} and that are compatible
     * with the passed {@link DistributionSetType} and UPDATE permission.
     *
     * @param rsql filter definition in RSQL syntax
     * @param distributionSetIdTypeId ID of the {@link DistributionSetType} the targets need to be
     *         compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndCompatibleAndUpdatable(@NotEmpty String rsql, @NotNull Long distributionSetIdTypeId);

    /**
     * Count all targets with failed actions for specific Rollout and that are
     * compatible with the passed {@link DistributionSetType} and created after
     * given timestamp
     *
     * @param rolloutId rolloutId of the rollout to be retried.
     * @param dsTypeId ID of the {@link DistributionSetType} the targets need to be
     *         compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByFailedInRollout(@NotEmpty String rolloutId, @NotNull Long dsTypeId);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query.
     *
     * @param targetFilterQueryId {@link TargetFilterQuery#getId()}
     * @return the found number of {@link Target}s
     * @throws EntityNotFoundException if {@link TargetFilterQuery} with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByTargetFilterQuery(long targetFilterQueryId);

    /**
     * Counts all {@link Target}s in the repository.
     *
     * @return number of targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long count();

    /**
     * creating a new {@link Target}.
     *
     * @param create to be created
     * @return the created {@link Target}
     * @throws EntityAlreadyExistsException given target already exists.
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TargetCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Target create(@NotNull @Valid TargetCreate create);

    /**
     * creates multiple {@link Target}s. If the given {@link Target}s already exists
     * in the DB an {@link EntityAlreadyExistsException} is thrown. {@link Target}s
     * contain all objects of the parameter targets, including duplicates.
     *
     * @param creates to be created.
     * @return the created {@link Target}s
     * @throws EntityAlreadyExistsException of one of the given targets already exist.
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TargetCreate}
     *         for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    List<Target> create(@NotNull @Valid Collection<TargetCreate> creates);

    /**
     * Deletes all targets with the given IDs.
     *
     * @param ids the IDs of the targets to be deleted
     * @throws EntityNotFoundException if (at least one) of the given target IDs does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void delete(@NotEmpty Collection<Long> ids);

    /**
     * Deletes target with the given controller ID.
     *
     * @param controllerId the controller ID of the target to be deleted
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_TARGET)
    void deleteByControllerID(@NotEmpty String controllerId);

    /**
     * Finds all targets for all the given parameter {@link TargetFilterQuery} and
     * that don't have the specified distribution set in their action history and
     * are compatible with the passed {@link DistributionSetType}.
     *
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsql filter definition in RSQL syntax
     * @param pageable the pageable to enhance the query for paging and sorting
     * @return a page of the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
            long distributionSetId, @NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Counts all targets for all the given parameter {@link TargetFilterQuery} and
     * that don't have the specified distribution set in their action history and
     * are compatible with the passed {@link DistributionSetType}.
     *
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsql filter definition in RSQL syntax
     * @return the count of found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    long countByRsqlAndNonDSAndCompatibleAndUpdatable(long distributionSetId, @NotNull String rsql);

    /**
     * Finds all targets for all the given parameter {@link TargetFilterQuery} and
     * that are not assigned to one of the {@link RolloutGroup}s and are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param groups the list of {@link RolloutGroup}s
     * @param rsql filter definition in RSQL syntax
     * @param distributionSetType type of the {@link DistributionSet} the targets must be compatible
     *         withs
     * @param pageable the pageable to enhance the query for paging and sorting
     * @return a page of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Slice<Target> findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            @NotEmpty Collection<Long> groups, @NotNull String rsql, @NotNull DistributionSetType distributionSetType,
            @NotNull Pageable pageable);

    /**
     * Counts all targets for all the given parameter {@link TargetFilterQuery} and
     * that are not assigned to one of the {@link RolloutGroup}s and are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param rsql filter definition in RSQL syntax
     * @param groups the list of {@link RolloutGroup}s
     * @param distributionSetType type of the {@link DistributionSet} the targets must be compatible with
     * @return count of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    long countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
            @NotNull String rsql, @NotEmpty Collection<Long> groups, @NotNull DistributionSetType distributionSetType);

    /**
     * Finds all targets with failed actions for specific Rollout and that are not
     * assigned to one of the retried {@link RolloutGroup}s and are compatible with
     * the passed {@link DistributionSetType}.
     *
     * @param rolloutId rolloutId of the rollout to be retried.
     * @param groups the list of {@link RolloutGroup}s
     * @param pageable the pageable to enhance the query for paging and sorting
     * @return a page of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Slice<Target> findByFailedRolloutAndNotInRolloutGroups(
            @NotNull String rolloutId, @NotEmpty Collection<Long> groups, @NotNull Pageable pageable);

    /**
     * Counts all targets with failed actions for specific Rollout and that are not
     * assigned to one of the {@link RolloutGroup}s and are compatible with the
     * passed {@link DistributionSetType}.
     *
     * @param rolloutId rolloutId of the rollout to be retried.
     * @param groups the list of {@link RolloutGroup}s
     * @return count of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    long countByFailedRolloutAndNotInRolloutGroups(@NotNull String rolloutId, @NotEmpty Collection<Long> groups);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    Slice<Target> findByRsqlAndNoOverridingActionsAndNotInRolloutAndCompatibleAndUpdatable(
            final long rolloutId, @NotNull String rsql, @NotNull DistributionSetType distributionSetType, @NotNull Pageable pageable);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ_AND_TARGET_READ)
    long countByActionsInRolloutGroup(final long rolloutGroupId);

    /**
     * Finds all targets of the provided {@link RolloutGroup} that have no Action
     * for the RolloutGroup.
     *
     * @param group the {@link RolloutGroup}
     * @param pageable the pageable to enhance the query for paging and sorting
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if rollout group with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByInRolloutGroupWithoutAction(long group, @NotNull Pageable pageable);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param pageable page parameter
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByAssignedDistributionSet(long distributionSetId, @NotNull Pageable pageable);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet} possible including additional filtering based on the given {@code spec}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsql the specification to filter the result set
     * @param pageable page parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByAssignedDistributionSetAndRsql(long distributionSetId, @NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Find {@link Target}s based a given IDs.
     *
     * @param controllerIDs to look for.
     * @return List of found{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Target> getByControllerID(@NotEmpty Collection<String> controllerIDs);

    /**
     * Find a {@link Target} based a given ID.
     *
     * @param controllerId to look for.
     * @return {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<Target> getByControllerID(@NotEmpty String controllerId);

    /**
     * Gets a {@link Target} based a given controller id and includes the details specified by the details key.
     *
     * @param controllerId to look for.
     * @param detailsKey the key of the details to include, e.g. {@link #DETAILS_AUTO_CONFIRMATION_STATUS}
     * @return {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Target getWithDetails(@NotEmpty String controllerId, String detailsKey);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    default Target getWithDetails(@NotEmpty String controllerId) {
        return getWithDetails(controllerId, DETAILS_BASE);
    }

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    default Target getWithAutoConfigurationStatus(@NotEmpty String controllerId) {
        return getWithDetails(controllerId, DETAILS_AUTO_CONFIRMATION_STATUS);
    }

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    default Target getWithTags(@NotEmpty String controllerId) {
        return getWithDetails(controllerId, DETAILS_TAGS);
    }

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    default Target getWithActions(@NotEmpty String controllerId) {
        return getWithDetails(controllerId, DETAILS_ACTIONS);
    }

    /**
     * Filter {@link Target}s for all the given parameters. If all parameters except
     * pageable are null, all available {@link Target}s are returned.
     *
     * @param filterParams the filters to apply; only filters are enabled that have non-null
     *         value; filters are AND-gated
     * @param pageable page parameters
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByFilters(@NotNull FilterParams filterParams, @NotNull Pageable pageable);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param pageReq page parameter
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByInstalledDistributionSet(long distributionSetId, @NotNull Pageable pageReq);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet} including
     * additional filtering based on the given {@code spec}.
     *
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsql the specification to filter the result
     * @param pageReq page parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByInstalledDistributionSetAndRsql(long distributionSetId, @NotNull String rsql, @NotNull Pageable pageReq);

    /**
     * Retrieves the {@link Target} which have a certain {@link TargetUpdateStatus}.
     *
     * @param status the {@link TargetUpdateStatus} to be filtered on
     * @param pageable page parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByUpdateStatus(@NotNull TargetUpdateStatus status, @NotNull Pageable pageable);

    /**
     * Retrieves all targets.
     *
     * @param pageable pagination parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all targets.
     *
     * @param rsql in RSQL notation
     * @param pageable pagination parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByRsql(@NotNull String rsql, @NotNull Pageable pageable);

    /**
     * Retrieves all target based on {@link TargetFilterQuery}.
     *
     * @param targetFilterQueryId {@link TargetFilterQuery#getId()}
     * @param pageable pagination parameter
     * @return the found {@link Target}s, never {@code null}
     * @throws EntityNotFoundException if {@link TargetFilterQuery} with given ID does not exist.
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByTargetFilterQuery(long targetFilterQueryId, @NotNull Pageable pageable);

    /**
     * Find targets by tag name.
     *
     * @param tagId tag ID
     * @param pageable the page request parameter for paging and sorting the result
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByTag(long tagId, @NotNull Pageable pageable);

    /**
     * Find targets by tag name.
     *
     * @param rsql in RSQL notation
     * @param tagId tag ID
     * @param pageable the page request parameter for paging and sorting the result
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByRsqlAndTag(@NotNull String rsql, long tagId, @NotNull Pageable pageable);

    /**
     * Verify if a target matches a specific target filter query, does not have a
     * specific DS already assigned and is compatible with it.
     *
     * @param controllerId of the {@link org.eclipse.hawkbit.repository.model.Target} to check
     * @param distributionSetId of the {@link org.eclipse.hawkbit.repository.model.DistributionSet} to consider
     * @param targetFilterQuery to execute
     * @return true if it matches
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
            @NotNull String controllerId, long distributionSetId, @NotNull String targetFilterQuery);

    /**
     * Initiates {@link TargetType} assignment to given {@link Target}s. If some targets in the list have the {@link TargetType}
     * not yet assigned, they will get assigned. If all targets are already of that type, there will be no un-assignment.
     *
     * @param controllerIds to set the type to
     * @param typeId to assign targets to
     * @return {@link TargetTypeAssignmentResult} with all meta-data of the assignment outcome.
     * @throws EntityNotFoundException if target type with given id does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTypeAssignmentResult assignType(@NotEmpty Collection<String> controllerIds, @NotNull Long typeId);

    /**
     * Initiates {@link TargetType} un-assignment to given {@link Target}s. The type of the targets will be set to {@code null}
     *
     * @param controllerIds to remove the type from
     * @return {@link TargetTypeAssignmentResult} with all meta-data of the assignment outcome.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTypeAssignmentResult unassignType(@NotEmpty Collection<String> controllerIds);

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to assign for
     * @param targetTagId to assign
     * @param notFoundHandler if not all targets found - if null - exception, otherwise tag what found and the handler is called with what's not found
     * @return list of assigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    List<Target> assignTag(@NotEmpty Collection<String> controllerIds, long targetTagId, final Consumer<Collection<String>> notFoundHandler);

    /**
     * Assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to assign for
     * @param targetTagId to assign
     * @return list of assigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_UPDATE_TARGET)
    List<Target> assignTag(@NotEmpty Collection<String> controllerIds, long targetTagId);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to un-assign for
     * @param targetTagId to un-assign
     * @param notFoundHandler if not all targets found - if null - exception, otherwise un-tag what found and the handler is called with what's not found
     * @return list of unassigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    List<Target> unassignTag(@NotEmpty Collection<String> controllerIds, long targetTagId, final Consumer<Collection<String>> notFoundHandler);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}s.
     *
     * @param controllerIds to un-assign for
     * @param targetTagId to un-assign
     * @return list of unassigned targets
     * @throws EntityNotFoundException if given targetTagId or at least one of the targets do not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    List<Target> unassignTag(@NotEmpty Collection<String> controllerIds, long targetTagId);

    /**
     * Un-assign a {@link TargetType} assignment to given {@link Target}.
     *
     * @param controllerId to un-assign for
     * @return the unassigned target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Target unassignType(@NotEmpty String controllerId);

    /**
     * Assign a {@link TargetType} assignment to given {@link Target}.
     *
     * @param controllerId to un-assign for
     * @param targetTypeId Target type id
     * @return the unassigned target
     * @throws EntityNotFoundException if TargetType with given target ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Target assignType(@NotEmpty String controllerId, @NotNull Long targetTypeId);

    /**
     * Assigns the target group to a given {@link Target}.
     *
     * @param controllerId to be updated
     * @param group - to be assigned to target
     * @return updated target
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void assignTargetGroup(@NotEmpty String controllerId, String group);

    /**
     * Finds targets by group or subgroup.
     * @param group - provided group/subgroup to filter for
     * @param withSubgroups - whether is a subgroup or not e.g. x/y/z
     * @param pageable - page parameter
     * @return all matching targets to provided group/subgroup
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findTargetsByGroup(@NotEmpty String group, boolean withSubgroups, @NotNull Pageable pageable);
    /**
     * Finds all the distinct target groups in the scope of a tenant
     *
     * @return list of all distinct target groups
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<String> findGroups();

    /**
     * Assigns the target group of the targets matching the provided rsql filter.
     *
     * @param group target group parameter
     * @param rsql rsql filter for {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void assignTargetGroupWithRsql(String group, @NotNull String rsql);

    /**
     * Assigns the provided group to the targets which are in the provided list of controllerIds.
     *
     * @param group target group parameter
     * @param controllerIds list of targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void assignTargetsWithGroup(String group, @NotEmpty List<String> controllerIds);

    /**
     * updates the {@link Target}.
     *
     * @param update to be updated
     * @return the updated {@link Target}
     * @throws EntityNotFoundException if given target does not exist
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TargetUpdate}
     *         for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Target update(@NotNull @Valid TargetUpdate update);

    /**
     * Find a {@link Target} based a given ID.
     *
     * @param id to look for
     * @return {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<Target> get(long id);

    /**
     * Retrieves all targets.
     *
     * @param ids the ids to for
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Target> get(@NotNull Collection<Long> ids);

    /**
     * Verifies that {@link Target} with given controller ID exists in the repository.
     *
     * @param controllerId of target
     * @return {@code true} if target with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    boolean existsByControllerId(@NotEmpty String controllerId);

    /**
     * Finds a single target tags its id.
     *
     * @param controllerId of the {@link Target}
     * @return the found Tag set
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Set<TargetTag> getTags(@NotEmpty String controllerId);

    /**
     * Get controller attributes of given {@link Target}.
     *
     * @param controllerId of the target
     * @return controller attributes as key/value pairs
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Map<String, String> getControllerAttributes(@NotEmpty String controllerId);

    /**
     * Trigger given {@link Target} to update its attributes.
     *
     * @param controllerId of the target
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void requestControllerAttributes(@NotEmpty String controllerId);

    /**
     * Check if update of given {@link Target} attributes is already requested.
     *
     * @param controllerId of target
     * @return {@code true}: update of controller attributes triggered.
     *         {@code false}: update of controller attributes not requested.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    boolean isControllerAttributesRequested(@NotEmpty String controllerId);

    /**
     * Retrieves {@link Target}s where
     * {@link #isControllerAttributesRequested(String)}.
     *
     * @param pageable page parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByControllerAttributesRequested(@NotNull Pageable pageable);

    /**
     * Creates a list of target meta-data entries.
     *
     * @param controllerId {@link Target} controller id the meta-data has to be created for
     * @param metadata the meta-data entries to create or update
     * @throws EntityNotFoundException if given target does not exist
     * @throws EntityAlreadyExistsException in case one of the metad-ata entry already exists for the specific key
     * @throws AssignmentQuotaExceededException if the maximum number of meta-data entries is exceeded for the addressed {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void createMetadata(@NotEmpty String controllerId, @NotEmpty Map<String, String> metadata);

    /**
     * Finds a single target meta-data by its id.
     *
     * @param controllerId of the {@link Target}
     * @return the found target meta-data
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Map<String, String> getMetadata(@NotEmpty String controllerId);

    /**
     * Updates a target meta-data value if corresponding entry exists.
     *
     * @param controllerId {@link Target} controller id of the meta-data entry to be updated
     * @param key meta data-entry key to be updated
     * @param value meta data-entry to be new value
     * @throws EntityNotFoundException in case the meta-data entry does not exist and cannot be updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void updateMetadata(@NotEmpty String controllerId, @NotNull String key, @NotNull String value);

    /**
     * Deletes a target meta data entry.
     *
     * @param controllerId where meta-data has to be deleted
     * @param key of the meta data element
     * @throws EntityNotFoundException if given target does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteMetadata(@NotEmpty String controllerId, @NotEmpty String key);
}