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
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
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

    /**
     * Counts number of targets with the given distribution set assigned.
     *
     * @param distributionSetId to search for
     * @return number of found {@link Target}s.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
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
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByInstalledDistributionSet(long distributionSetId);

    /**
     * Checks if there is already a {@link Target} that has the given distribution
     * set Id assigned or installed.
     *
     * @param distributionSetId to search for
     * @return <code>true</code> if a {@link Target} exists.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean existsByInstalledOrAssignedDistributionSet(long distributionSetId);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query.
     *
     * @param rsqlParam filter definition in RSQL syntax
     * @return the found number of {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsql(@NotEmpty String rsqlParam);

    /**
     * Count {@link TargetFilterQuery}s for given target filter query with UPDATE permission.
     *
     * @param rsqlParam filter definition in RSQL syntax
     * @return the found number of {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndUpdatable(@NotEmpty String rsqlParam);

    /**
     * Count all targets for given {@link TargetFilterQuery} and that are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param rsqlParam filter definition in RSQL syntax
     * @param distributionSetIdTypeId ID of the {@link DistributionSetType} the targets need to be
     *         compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndCompatible(@NotEmpty String rsqlParam, @NotNull Long distributionSetIdTypeId);

    /**
     * Count all targets for given {@link TargetFilterQuery} and that are compatible
     * with the passed {@link DistributionSetType} and UPDATE permission.
     *
     * @param rsqlParam filter definition in RSQL syntax
     * @param distributionSetIdTypeId ID of the {@link DistributionSetType} the targets need to be
     *         compatible with
     * @return the found number of{@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByRsqlAndCompatibleAndUpdatable(@NotEmpty String rsqlParam, @NotNull Long distributionSetIdTypeId);

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
     * @throws ConstraintViolationException if fields are not filled as specified. Check {@link TargetCreate}
     *         for field constraints.
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
     * @param pageRequest the pageRequest to enhance the query for paging and sorting
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsqlParam filter definition in RSQL syntax
     * @return a page of the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(@NotNull Pageable pageRequest,
            long distributionSetId, @NotNull String rsqlParam);

    /**
     * Counts all targets for all the given parameter {@link TargetFilterQuery} and
     * that don't have the specified distribution set in their action history and
     * are compatible with the passed {@link DistributionSetType}.
     *
     * @param distributionSetId id of the {@link DistributionSet}
     * @param rsqlParam filter definition in RSQL syntax
     * @return the count of found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    long countByRsqlAndNonDSAndCompatibleAndUpdatable(long distributionSetId, @NotNull String rsqlParam);

    /**
     * Finds all targets for all the given parameter {@link TargetFilterQuery} and
     * that are not assigned to one of the {@link RolloutGroup}s and are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param pageRequest the pageRequest to enhance the query for paging and sorting
     * @param groups the list of {@link RolloutGroup}s
     * @param targetFilterQuery filter definition in RSQL syntax
     * @param distributionSetType type of the {@link DistributionSet} the targets must be compatible
     *         withs
     * @return a page of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Slice<Target> findByTargetFilterQueryAndNotInRolloutGroupsAndCompatibleAndUpdatable(@NotNull Pageable pageRequest,
            @NotEmpty Collection<Long> groups, @NotNull String targetFilterQuery,
            @NotNull DistributionSetType distributionSetType);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Slice<Target> findByNotInGEGroupAndNotInActiveActionGEWeightOrInRolloutAndTargetFilterQueryAndCompatibleAndUpdatable(
            @NotNull Pageable pageRequest, final long rolloutId, final int weight, final long firstGroupId, @NotNull String targetFilterQuery,
            @NotNull DistributionSetType distributionSetType);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByActionsInRolloutGroup(final long rolloutGroupId);

    /**
     * Finds all targets with failed actions for specific Rollout and that are not
     * assigned to one of the retried {@link RolloutGroup}s and are compatible with
     * the passed {@link DistributionSetType}.
     *
     * @param pageRequest the pageRequest to enhance the query for paging and sorting
     * @param groups the list of {@link RolloutGroup}s
     * @param rolloutId rolloutId of the rollout to be retried.
     * @return a page of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByFailedRolloutAndNotInRolloutGroups(@NotNull Pageable pageRequest,
            @NotEmpty Collection<Long> groups, @NotNull String rolloutId);

    /**
     * Counts all targets for all the given parameter {@link TargetFilterQuery} and
     * that are not assigned to one of the {@link RolloutGroup}s and are compatible
     * with the passed {@link DistributionSetType}.
     *
     * @param groups the list of {@link RolloutGroup}s
     * @param rsqlParam filter definition in RSQL syntax
     * @param distributionSetType type of the {@link DistributionSet} the targets must be compatible
     *         with
     * @return count of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    long countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(@NotEmpty Collection<Long> groups,
            @NotNull String rsqlParam, @NotNull DistributionSetType distributionSetType);

    /**
     * Counts all targets with failed actions for specific Rollout and that are not
     * assigned to one of the {@link RolloutGroup}s and are compatible with the
     * passed {@link DistributionSetType}.
     *
     * @param groups the list of {@link RolloutGroup}s
     * @param rolloutId rolloutId of the rollout to be retried.
     * @return count of the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    long countByFailedRolloutAndNotInRolloutGroups(@NotEmpty Collection<Long> groups, @NotNull String rolloutId);

    /**
     * Finds all targets of the provided {@link RolloutGroup} that have no Action
     * for the RolloutGroup.
     *
     * @param pageRequest the pageRequest to enhance the query for paging and sorting
     * @param group the {@link RolloutGroup}
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if rollout group with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByInRolloutGroupWithoutAction(@NotNull Pageable pageRequest, long group);

    /**
     * retrieves {@link Target}s by the assigned {@link DistributionSet}.
     *
     * @param pageReq page parameter
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByAssignedDistributionSet(@NotNull Pageable pageReq, long distributionSetId);

    /**
     * Retrieves {@link Target}s by the assigned {@link DistributionSet} possible
     * including additional filtering based on the given {@code spec}.
     *
     * @param pageReq page parameter
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsqlParam the specification to filter the result set
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByAssignedDistributionSetAndRsql(@NotNull Pageable pageReq, long distributionSetId,
            @NotNull String rsqlParam);

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
     * Filter {@link Target}s for all the given parameters. If all parameters except
     * pageable are null, all available {@link Target}s are returned.
     *
     * @param pageable page parameters
     * @param filterParams the filters to apply; only filters are enabled that have non-null
     *         value; filters are AND-gated
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByFilters(@NotNull Pageable pageable, @NotNull FilterParams filterParams);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet}.
     *
     * @param pageReq page parameter
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @return the found {@link Target}s
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByInstalledDistributionSet(@NotNull Pageable pageReq, long distributionSetId);

    /**
     * retrieves {@link Target}s by the installed {@link DistributionSet} including
     * additional filtering based on the given {@code spec}.
     *
     * @param pageReq page parameter
     * @param distributionSetId the ID of the {@link DistributionSet}
     * @param rsqlParam the specification to filter the result
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    Page<Target> findByInstalledDistributionSetAndRsql(@NotNull Pageable pageReq, long distributionSetId,
            @NotNull String rsqlParam);

    /**
     * Retrieves the {@link Target} which have a certain {@link TargetUpdateStatus}.
     *
     * @param pageable page parameter
     * @param status the {@link TargetUpdateStatus} to be filtered on
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByUpdateStatus(@NotNull Pageable pageable, @NotNull TargetUpdateStatus status);

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
     * @param pageable pagination parameter
     * @param rsqlParam in RSQL notation
     * @return the found {@link Target}s, never {@code null}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);

    /**
     * Retrieves all target based on {@link TargetFilterQuery}.
     *
     * @param pageable pagination parameter
     * @param targetFilterQueryId {@link TargetFilterQuery#getId()}
     * @return the found {@link Target}s, never {@code null}
     * @throws EntityNotFoundException if {@link TargetFilterQuery} with given ID does not exist.
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Slice<Target> findByTargetFilterQuery(@NotNull Pageable pageable, long targetFilterQueryId);

    /**
     * Find targets by tag name.
     *
     * @param pageable the page request parameter for paging and sorting the result
     * @param tagId tag ID
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByTag(@NotNull Pageable pageable, long tagId);

    /**
     * Find targets by tag name.
     *
     * @param pageable the page request parameter for paging and sorting the result
     * @param tagId tag ID
     * @param rsqlParam in RSQL notation
     * @return list of matching targets
     * @throws EntityNotFoundException if target tag with given ID does not exist
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByRsqlAndTag(@NotNull Pageable pageable, @NotNull String rsqlParam, long tagId);

    /**
     * Initiates {@link TargetType} assignment to given {@link Target}s. If some
     * targets in the list have the {@link TargetType} not yet assigned, they will
     * get assigned. If all targets are already of that type, there will be no
     * un-assignment.
     *
     * @param controllerIds to set the type to
     * @param typeId to assign targets to
     * @return {@link TargetTypeAssignmentResult} with all metadata of the
     *         assignment outcome.
     * @throws EntityNotFoundException if target type with given id does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTypeAssignmentResult assignType(@NotEmpty Collection<String> controllerIds, @NotNull Long typeId);

    /**
     * Initiates {@link TargetType} un-assignment to given {@link Target}s. The type
     * of the targets will be set to {@code null}
     *
     * @param controllerIds to remove the type from
     * @return {@link TargetTypeAssignmentResult} with all metadata of the
     *         assignment outcome.
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
    default List<Target> assignTag(@NotEmpty Collection<String> controllerIds, long targetTagId) {
        return assignTag(controllerIds, targetTagId, null);
    }

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
    default List<Target> unassignTag(@NotEmpty Collection<String> controllerIds, long targetTagId) {
        return unassignTag(controllerIds, targetTagId, null);
    }

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
     * @param pageReq page parameter
     * @return the found {@link Target}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Target> findByControllerAttributesRequested(@NotNull Pageable pageReq);

    /**
     * Verifies that {@link Target} with given controller ID exists in the
     * repository.
     *
     * @param controllerId of target
     * @return {@code true} if target with given ID exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    boolean existsByControllerId(@NotEmpty String controllerId);

    /**
     * Verify if a target matches a specific target filter query, does not have a
     * specific DS already assigned and is compatible with it.
     *
     * @param controllerId of the {@link org.eclipse.hawkbit.repository.model.Target} to
     *         check
     * @param distributionSetId of the
     *         {@link org.eclipse.hawkbit.repository.model.DistributionSet} to
     *         consider
     * @param targetFilterQuery to execute
     * @return true if it matches
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY_AND_READ_TARGET)
    boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(@NotNull String controllerId,
            long distributionSetId, @NotNull String targetFilterQuery);

    /**
     * Finds a single target tags its id.
     *
     * @param controllerId of the {@link Target}
     * @return the found Tag set
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Set<TargetTag> getTagsByControllerId(@NotEmpty String controllerId);

    /**
     * Creates a list of target meta data entries.
     *
     * @param controllerId {@link Target} controller id the metadata has to be created for
     * @param metadata the meta data entries to create or update
     * @return the updated or created target metadata entries
     * @throws EntityNotFoundException if given target does not exist
     * @throws EntityAlreadyExistsException in case one of the metadata entry already exists for the specific
     *         key
     * @throws AssignmentQuotaExceededException if the maximum number of {@link MetaData} entries is exceeded for
     *         the addressed {@link Target}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<TargetMetadata> createMetaData(@NotEmpty String controllerId, @NotEmpty Collection<MetaData> metadata);

    /**
     * Deletes a target meta data entry.
     *
     * @param controllerId where metadata has to be deleted
     * @param key of the meta data element
     * @throws EntityNotFoundException if given target does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteMetaData(@NotEmpty String controllerId, @NotEmpty String key);

    /**
     * Finds all meta data by the given target id.
     *
     * @param pageable the page request to page the result
     * @param controllerId the controller id to retrieve the metadata from
     * @return a paged result of all meta data entries for a given target id
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<TargetMetadata> findMetaDataByControllerId(@NotNull Pageable pageable, @NotEmpty String controllerId);

    /**
     * Counts all meta data by the given target id.
     *
     * @param controllerId the controller id to retrieve the meta data from
     * @return count of all meta data entries for a given target id
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countMetaDataByControllerId(@NotEmpty String controllerId);

    /**
     * Finds all metadata by the given target id and query.
     *
     * @param pageable the page request to page the result
     * @param controllerId the controller id to retrieve the metadata from
     * @param rsqlParam rsql query string
     * @return a paged result of all meta data entries for a given target id
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<TargetMetadata> findMetaDataByControllerIdAndRsql(@NotNull Pageable pageable, @NotEmpty String controllerId,
            @NotNull String rsqlParam);

    /**
     * Finds a single target meta data by its id.
     *
     * @param controllerId of the {@link Target}
     * @param key of the meta data element
     * @return the found TargetMetadata
     * @throws EntityNotFoundException if target with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<TargetMetadata> getMetaDataByControllerId(@NotEmpty String controllerId, @NotEmpty String key);

    /**
     * Updates a target meta data value if corresponding entry exists.
     *
     * @param controllerId {@link Target} controller id of the metadata entry to be updated
     * @param metadata meta data entry to be updated
     * @return the updated meta data entry
     * @throws EntityNotFoundException in case the metadata entry does not exist and cannot be updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    TargetMetadata updateMetadata(@NotEmpty String controllerId, @NotNull MetaData metadata);

    /**
     * Toggles {@link TargetTag} assignment to given {@link Target}s by means that
     * if some (or all) of the targets in the list have the {@link Tag} not yet
     * assigned, they will be. Only if all of them have the tag already assigned
     * they will be removed instead.
     *
     * @param controllerIds to toggle for
     * @param tagName to toggle
     * @return TagAssigmentResult with all metadata of the assignment outcome.
     * @throws EntityNotFoundException if tag with given name does not exist
     * @deprecated since 0.6.0 - not very usable with very unclear logic
     */
    @Deprecated(forRemoval = true, since = "0.6.0")
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    TargetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<String> controllerIds, @NotEmpty String tagName);

    /**
     * Un-assign a {@link TargetTag} assignment to given {@link Target}.
     *
     * @param controllerId to un-assign for
     * @param targetTagId to un-assign
     * @return the unassigned target or <null> if no target is unassigned
     * @throws EntityNotFoundException if TAG with given ID does not exist
     * @deprecated since 0.6.0 - use {@link #unassignTag(Collection, long)} (List, long)} instead
     */
    @Deprecated(forRemoval = true, since = "0.6.0")
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    Target unassignTag(@NotEmpty String controllerId, long targetTagId);
}