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
import java.util.Optional;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSet}s.
 *
 */
public interface DistributionSetManagement
        extends RepositoryManagement<DistributionSet, DistributionSetCreate, DistributionSetUpdate> {

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param id
     *            to assign and update
     * @param moduleIds
     *            to get assigned
     *
     * @return the updated {@link DistributionSet}.
     *
     * @throws EntityNotFoundException
     *             if (at least one) given module does not exist
     *
     * @throws EntityReadOnlyException
     *             if use tries to change the {@link DistributionSet} s while
     *             the DS is already in use.
     *
     * @throws UnsupportedSoftwareModuleForThisDistributionSetException
     *             if {@link SoftwareModule#getType()} is not supported by this
     *             {@link DistributionSet#getType()}.
     *
     * @throws AssignmentQuotaExceededException
     *             if the maximum number of {@link SoftwareModule}s is exceeded
     *             for the addressed {@link DistributionSet}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet assignSoftwareModules(long id, @NotEmpty Collection<Long> moduleIds);

    /**
     * Assign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}s.
     *
     * @param ids
     *            to assign for
     * @param tagId
     *            to assign
     *
     * @return list of assigned ds
     *
     * @throws EntityNotFoundException
     *             if tag with given ID does not exist or (at least one) of the
     *             distribution sets.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> assignTag(@NotEmpty Collection<Long> ids, long tagId);

    /**
     * Creates a list of distribution set meta data entries.
     *
     * @param id
     *            if the {@link DistributionSet} the metadata has to be created
     *            for
     * @param metadata
     *            the meta data entries to create or update
     * @return the updated or created distribution set meta data entries
     *
     * @throws EntityNotFoundException
     *             if given set does not exist
     *
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     *
     * @throws AssignmentQuotaExceededException
     *             if the maximum number of {@link MetaData} entries is exceeded
     *             for the addressed {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSetMetadata> createMetaData(long id, @NotEmpty Collection<MetaData> metadata);

    /**
     * Deletes a distribution set meta data entry.
     *
     * @param id
     *            where meta data has to be deleted
     * @param key
     *            of the meta data element
     *
     * @throws EntityNotFoundException
     *             if given set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteMetaData(long id, @NotEmpty String key);

    /**
     * Locks a distribution set. From then on its functional properties could not be changed and
     * it could be assigned to targets
     *
     * @param id the distribution set id
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void lock(final long id);

    /**
     * Unlocks a distribution set.<br/>
     * Use it with extreme care! In general once distribution set is locked
     * it shall not be unlocked. Note that it could have been assigned / deployed to targets.
     *
     * @param id the distribution set id
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void unlock(final long id);

    /**
     * Retrieves the distribution set for a given action.
     *
     * @param actionId
     *            the action associated with the distribution set
     * @return the distribution set which is associated with the action
     *
     * @throws EntityNotFoundException
     *             if action with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> getByAction(long actionId);

    /**
     * Find {@link DistributionSet} based on given ID including (lazy loaded)
     * details, e.g. {@link DistributionSet#getModules()}.
     *
     * Note: for performance reasons it is recommended to use {@link #get(Long)}
     * if details are not necessary.
     *
     * @param id
     *            to look for.
     *
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> getWithDetails(long id);

    /**
     * Find distribution set by name and version.
     *
     * @param distributionName
     *            name of {@link DistributionSet}; case insensitive
     * @param version
     *            version of {@link DistributionSet}
     *
     * @return the page with the found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> getByNameAndVersion(@NotEmpty String distributionName, @NotEmpty String version);

    /**
     * Find distribution set by id and throw an exception if it is deleted,
     * incomplete or invalidated.
     *
     * @param id
     *            id of {@link DistributionSet}
     *
     * @return the found valid {@link DistributionSet}
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     *
     * @throws InvalidDistributionSetException
     *             if distribution set with given ID is invalidated
     *
     * @throws IncompleteDistributionSetException
     *             if distribution set with given ID is incomplete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getValidAndComplete(long id);

    /**
     * Find distribution set by id and throw an exception if it is deleted or
     * invalidated.
     *
     * @param id
     *            id of {@link DistributionSet}
     *
     * @return the found valid {@link DistributionSet}
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     *
     * @throws InvalidDistributionSetException
     *             if distribution set with given ID is invalidated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getValid(long id);

    /**
     * Find distribution set by id and throw an exception if it is (soft)
     * deleted.
     *
     * @param id
     *            id of {@link DistributionSet}
     *
     * @return the found valid {@link DistributionSet}
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getOrElseThrowException(long id);

    /**
     * Finds all meta data by the given distribution set id.
     *
     * @param pageable
     *            the page request to page the result
     * @param id
     *            the distribution set id to retrieve the meta data from
     *
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findMetaDataByDistributionSetId(@NotNull Pageable pageable, long id);

    /**
     * Counts all meta data by the given distribution set id.
     *
     * @param id
     *            the distribution set id to retrieve the meta data count from
     *
     * @return count of ds metadata
     *
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countMetaDataByDistributionSetId(long id);

    /**
     * Finds all meta data by the given distribution set id.
     *
     * @param pageable
     *            the page request to page the result
     * @param id
     *            the distribution set id to retrieve the meta data from
     * @param rsqlParam
     *            rsql query string
     *
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     *
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     *
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     *
     * @throws EntityNotFoundException
     *             of distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findMetaDataByDistributionSetIdAndRsql(@NotNull Pageable pageable,
                                                                         long id, @NotNull String rsqlParam);

    /**
     * Finds all {@link DistributionSet}s based on completeness.
     *
     * @param pageable
     *            the pagination parameter
     * @param complete
     *            to <code>true</code> for returning only completed distribution
     *            sets or <code>false</code> for only incomplete ones nor
     *            <code>null</code> to return both.
     *
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<DistributionSet> findByCompleted(@NotNull Pageable pageable, Boolean complete);

    /**
     * Counts all {@link DistributionSet}s based on completeness.
     *
     * @param complete
     *            to <code>true</code> for counting only completed distribution
     *            sets or <code>false</code> for only incomplete ones nor
     *            <code>null</code> to count both.
     *
     * @return count of all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByCompleted(Boolean complete);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param pageable
     *            page parameter
     * @param distributionSetFilter
     *            has details of filters to be applied.
     * @return the page of found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<DistributionSet> findByDistributionSetFilter(@NotNull Pageable pageable,
            @NotNull DistributionSetFilter distributionSetFilter);

    /**
     * Method retrieves all {@link DistributionSet}s from the repository in the
     * following order:
     * <p>
     * 1) {@link DistributionSet}s which have the given {@link Target} as
     * {@link TargetInfo#getInstalledDistributionSet()}
     * <p>
     * 2) {@link DistributionSet}s which have the given {@link Target} as
     * {@link Target#getAssignedDistributionSet()}
     * <p>
     * 3) {@link DistributionSet}s which have no connection to the given
     * {@link Target} ordered by ID of the DistributionSet.
     *
     * @param pageable
     *            the page request to page the result set *
     * @param distributionSetFilter
     *            has details of filters to be applied.
     * @param assignedOrInstalled
     *            the id of the Target to be ordered by
     *
     * @return {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<DistributionSet> findByDistributionSetFilterOrderByLinkedTarget(@NotNull Pageable pageable,
            @NotNull DistributionSetFilter distributionSetFilter, @NotEmpty String assignedOrInstalled);

    /**
     * Counts all {@link DistributionSet}s in repository based on given filter.
     *
     * @param distributionSetFilter
     *            has details of filters to be applied.
     *
     * @return count of {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByDistributionSetFilter(@NotNull DistributionSetFilter distributionSetFilter);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param pageable
     *            page parameter
     * @param tagId
     *            of the tag the DS are assigned to
     * @return the page of found {@link DistributionSet}
     *
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     *
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     *
     * @throws EntityNotFoundException
     *             of distribution set tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findByTag(@NotNull Pageable pageable, long tagId);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param pageable
     *            page parameter
     * @param rsqlParam
     *            rsql query string
     * @param tagId
     *            of the tag the DS are assigned to
     * @return the page of found {@link DistributionSet}
     *
     * @throws EntityNotFoundException
     *             of distribution set tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findByRsqlAndTag(@NotNull Pageable pageable, @NotNull String rsqlParam, long tagId);

    /**
     * Finds a single distribution set meta data by its id.
     *
     * @param id
     *            of the {@link DistributionSet}
     * @param key
     *            of the meta data element
     * @return the found DistributionSetMetadata
     *
     * @throws EntityNotFoundException
     *             is set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetMetadata> getMetaDataByDistributionSetId(long id, @NotEmpty String key);

    /**
     * Checks if a {@link DistributionSet} is currently in use by a target in
     * the repository.
     *
     * @param id
     *            to check
     *
     * @return <code>true</code> if in use
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean isInUse(long id);

    /**
     * Toggles {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}s by means that if some (or all) of the targets in
     * the list have the {@link Tag} not yet assigned, they will be. Only if all
     * of theme have the tag already assigned they will be removed instead.
     *
     * @param ids
     *            to toggle for
     * @param tagName
     *            to toggle
     * @return {@link DistributionSetTagAssignmentResult} with all meta data of
     *         the assignment outcome.
     *
     * @throws EntityNotFoundException
     *             if given tag does not exist or (at least one) module
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<Long> ids, @NotNull String tagName);

    /**
     * Unassigns a {@link SoftwareModule} form an existing
     * {@link DistributionSet}.
     *
     * @param id
     *            to get unassigned form
     * @param moduleId
     *            to be unassigned
     * @return the updated {@link DistributionSet}.
     *
     * @throws EntityNotFoundException
     *             if given module or DS does not exist
     *
     * @throws EntityReadOnlyException
     *             if use tries to change the {@link DistributionSet} s while
     *             the DS is already in use.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unassignSoftwareModule(long id, long moduleId);

    /**
     * Unassign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}.
     *
     * @param id
     *            to unassign for
     * @param tagId
     *            to unassign
     * @return the unassigned ds or <null> if no ds is unassigned
     *
     * @throws EntityNotFoundException
     *             if set or tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unassignTag(long id, long tagId);

    /**
     * Updates a distribution set meta data value if corresponding entry exists.
     *
     * @param id
     *            {@link DistributionSet} of the meta data entry to be updated
     * @param metadata
     *            meta data entry to be updated
     * @return the updated meta data entry
     *
     * @throws EntityNotFoundException
     *             in case the meta data entry does not exists and cannot be
     *             updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetMetadata updateMetaData(long id, @NotNull MetaData metadata);

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     *
     * @param typeId
     *            to look for
     *
     * @return number of {@link DistributionSet}s
     *
     * @throws EntityNotFoundException
     *             if type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByTypeId(long typeId);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Rollout}s by status for
     * Distribution Set.
     *
     * @param id
     *            to look for
     *
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Rollout}s status counts
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<Statistic> countRolloutsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Action}s by status for
     * Distribution Set.
     *
     * @param id
     *            to look for
     *
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Action}s status counts
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<Statistic> countActionsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate}s
     * for Distribution Set.
     *
     * @param id
     *            to look for
     *
     * @return number of {@link org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate}s
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countAutoAssignmentsForDistributionSet(@NotNull Long id);

    /**
     * Sets the specified {@link DistributionSet} as invalidated.
     *
     * @param distributionSet
     *            the ID of the {@link DistributionSet} to be set to invalid
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void invalidate(DistributionSet distributionSet);
}
