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
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSet}s.
 */
public interface DistributionSetManagement extends RepositoryManagement<DistributionSet, DistributionSetCreate, DistributionSetUpdate> {

    /**
     * Find {@link DistributionSet} based on given ID including (lazy loaded) details, e.g. {@link DistributionSet#getModules()}. <br/>
     * For performance reasons it is recommended to use {@link #get(long)} if the details are not required.
     *
     * @param id to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> getWithDetails(long id);

    /**
     * Find distribution set by id and throw an exception if it is (soft) deleted.
     *
     * @param id id of {@link DistributionSet}
     * @return the found valid {@link DistributionSet}
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getOrElseThrowException(long id);

    /**
     * Sets the specified {@link DistributionSet} as invalidated.
     *
     * @param distributionSet the ID of the {@link DistributionSet} to be set to invalid
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void invalidate(DistributionSet distributionSet);

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param id to assign and update
     * @param moduleIds to get assigned
     * @return the updated {@link DistributionSet}.
     * @throws EntityNotFoundException if (at least one) given module does not exist
     * @throws EntityReadOnlyException if tries to change the {@link DistributionSet} s while the DS is already in use.
     * @throws UnsupportedSoftwareModuleForThisDistributionSetException if {@link SoftwareModule#getType()} is not supported by this
     *         {@link DistributionSet#getType()}.
     * @throws AssignmentQuotaExceededException if the maximum number of {@link SoftwareModule}s is exceeded for the addressed
     *         {@link DistributionSet}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet assignSoftwareModules(long id, @NotEmpty Collection<Long> moduleIds);

    /**
     * Unassigns a {@link SoftwareModule} form an existing {@link DistributionSet}.
     *
     * @param id to get unassigned form
     * @param moduleId to be unassigned
     * @return the updated {@link DistributionSet}.
     * @throws EntityNotFoundException if given module or DS does not exist
     * @throws EntityReadOnlyException if use tries to change the {@link DistributionSet} s while the DS is already in use.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unassignSoftwareModule(long id, long moduleId);

    /**
     * Assign a {@link DistributionSetTag} assignment to given {@link DistributionSet}s.
     *
     * @param ids to assign for
     * @param tagId to assign
     * @return list of assigned ds
     * @throws EntityNotFoundException if tag with given ID does not exist or (at least one) of the distribution sets.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> assignTag(@NotEmpty Collection<Long> ids, long tagId);

    /**
     * Unassign a {@link DistributionSetTag} assignment to given {@link DistributionSet}s.
     *
     * @param ids to assign for
     * @param tagId to assign
     * @return list of assigned ds
     * @throws EntityNotFoundException if tag with given ID does not exist or (at least one) of the distribution sets.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> unassignTag(@NotEmpty Collection<Long> ids, long tagId);

    /**
     * Creates a map of distribution set meta-data entries.
     *
     * @param id if the {@link DistributionSet} the meta-data has to be created for
     * @param metadata the meta-data entries to create or update
     * @throws EntityNotFoundException if given set does not exist
     * @throws EntityAlreadyExistsException in case one of the meta-data entry already exists for the specific key
     * @throws AssignmentQuotaExceededException if the maximum number of meta-data entries is exceeded for the addressed {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void createMetadata(long id, @NotEmpty Map<String, String> metadata);

    /**
     * Finds all meta-data by the given distribution set id.
     *
     * @param id the distribution set id to retrieve the meta-data from
     * @return a paged result of all meta-data entries for a given distribution set id
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Map<String, String> getMetadata(long id);

    /**
     * Updates a distribution set meta-data values by adding them.
     *
     * @param id {@link DistributionSet} of the meta-data entry to be updated
     * @param key meta data-entry key to be updated
     * @param value meta data-entry to be new value
     * @throws EntityNotFoundException in case the meta-data entry does not exist and cannot be updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void updateMetadata(long id, @NotNull String key, @NotNull String value);

    /**
     * Deletes a distribution set meta-data entry.
     *
     * @param id where meta-data has to be deleted
     * @param key of the meta-data element
     * @throws EntityNotFoundException if given set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteMetadata(long id, @NotEmpty String key);

    /**
     * Locks a distribution set. From then on its functional properties could not be changed, and it could be assigned to targets
     *
     * @param id the distribution set id
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void lock(final long id);

    /**
     * Unlocks a distribution set.<br/>
     * Use it with extreme care! In general once distribution set is locked it shall not be unlocked. Note that it could have been assigned /
     * deployed to targets.
     *
     * @param id the distribution set id
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void unlock(final long id);

    /**
     * Find distribution set by id and throw an exception if it is deleted or invalidated.
     *
     * @param id id of {@link DistributionSet}
     * @return the found valid {@link DistributionSet}
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     * @throws InvalidDistributionSetException if distribution set with given ID is invalidated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getValid(long id);

    /**
     * Find distribution set by id and throw an exception if it is deleted, incomplete or invalidated.
     *
     * @param id id of {@link DistributionSet}
     * @return the found valid {@link DistributionSet}
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     * @throws InvalidDistributionSetException if distribution set with given ID is invalidated
     * @throws IncompleteDistributionSetException if distribution set with given ID is incomplete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet getValidAndComplete(long id);

    /**
     * Retrieves the distribution set for a given action.
     *
     * @param actionId the action associated with the distribution set
     * @return the distribution set which is associated with the action
     * @throws EntityNotFoundException if action with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findByAction(long actionId);

    /**
     * Find distribution set by name and version.
     *
     * @param distributionName name of {@link DistributionSet}; case insensitive
     * @param version version of {@link DistributionSet}
     * @return the page with the found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findByNameAndVersion(@NotEmpty String distributionName, @NotEmpty String version);

    /**
     * Finds all {@link DistributionSet}s based on completeness.
     *
     * @param complete to <code>true</code> for returning only completed distribution sets or <code>false</code> for only incomplete ones nor
     *         <code>null</code> to return both.
     * @param pageable the pagination parameter
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<DistributionSet> findByCompleted(Boolean complete, @NotNull Pageable pageable);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param distributionSetFilter has details of filters to be applied.
     * @param pageable page parameter
     * @return the page of found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Slice<DistributionSet> findByDistributionSetFilter(@NotNull DistributionSetFilter distributionSetFilter, @NotNull Pageable pageable);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param tagId of the tag the DS are assigned to
     * @param pageable page parameter
     * @return the page of found {@link DistributionSet}
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     * @throws EntityNotFoundException of distribution set tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findByTag(long tagId, @NotNull Pageable pageable);

    /**
     * Retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param rsql rsql query string
     * @param tagId of the tag the DS are assigned to
     * @param pageable page parameter
     * @return the page of found {@link DistributionSet}
     * @throws EntityNotFoundException of distribution set tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findByRsqlAndTag(@NotNull String rsql, long tagId, @NotNull Pageable pageable);

    /**
     * Counts all {@link DistributionSet}s based on completeness.
     *
     * @param complete to <code>true</code> for counting only completed distribution sets or <code>false</code> for only incomplete ones
     *         nor <code>null</code> to count both.
     * @return count of all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByCompleted(Boolean complete);

    /**
     * Counts all {@link DistributionSet}s in repository based on given filter.
     *
     * @param distributionSetFilter has details of filters to be applied.
     * @return count of {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByDistributionSetFilter(@NotNull DistributionSetFilter distributionSetFilter);

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     *
     * @param typeId to look for
     * @return number of {@link DistributionSet}s
     * @throws EntityNotFoundException if type with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countByTypeId(long typeId);

    /**
     * Checks if a {@link DistributionSet} is currently in use by a target in
     * the repository.
     *
     * @param id to check
     * @return <code>true</code> if in use
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean isInUse(long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Rollout}s by status for
     * Distribution Set.
     *
     * @param id to look for
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Rollout}s status counts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<Statistic> countRolloutsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.model.Action}s by status for
     * Distribution Set.
     *
     * @param id to look for
     * @return List of Statistics for {@link org.eclipse.hawkbit.repository.model.Action}s status counts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<Statistic> countActionsByStatusForDistributionSet(@NotNull Long id);

    /**
     * Count all {@link org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate}s
     * for Distribution Set.
     *
     * @param id to look for
     * @return number of {@link org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countAutoAssignmentsForDistributionSet(@NotNull Long id);
}