/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.DistributionSetCreationFailedMissingMandatoryModuleException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSet}s.
 *
 */
public interface DistributionSetManagement {

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param setId
     *            to assign and update
     * @param moduleIds
     *            to get assigned
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
     *             is {@link SoftwareModule#getType()} is not supported by this
     *             {@link DistributionSet#getType()}.
     * 
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet assignSoftwareModules(@NotNull Long setId, @NotEmpty Collection<Long> moduleIds);

    /**
     * Assign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}s.
     *
     * @param dsIds
     *            to assign for
     * @param tagId
     *            to assign
     * @return list of assigned ds
     * 
     * @throws EntityNotFoundException
     *             if tag with given ID does not exist or (at least one) of the
     *             distribution sets.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> assignTag(@NotEmpty Collection<Long> dsIds, @NotNull Long tagId);

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     *
     * @return number of {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetsAll();

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
    Long countDistributionSetsByType(@NotNull Long typeId);

    /**
     * @return number of {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetTypesAll();

    /**
     * Creates a new {@link DistributionSet}.
     *
     * @param create
     *            {@link DistributionSet} to be created
     * @return the new persisted {@link DistributionSet}
     *
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSet#getModules()} or
     *             {@link DistributionSet#getType()})
     * @throws DistributionSetCreationFailedMissingMandatoryModuleException
     *             is {@link DistributionSet} does not contain mandatory
     *             {@link SoftwareModule}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSet createDistributionSet(@NotNull DistributionSetCreate create);

    /**
     * creates a list of distribution set meta data entries.
     *
     * @param dsId
     *            if the {@link DistributionSet} the metadata has to be created
     *            for
     * @param metadata
     *            the meta data entries to create or update
     * @return the updated or created distribution set meta data entries
     * 
     * @throws EntityNotFoundException
     *             if given set does not exist
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSetMetadata> createDistributionSetMetadata(@NotNull Long dsId,
            @NotEmpty Collection<MetaData> metadata);

    /**
     * Creates multiple {@link DistributionSet}s.
     *
     * @param creates
     *            to be created
     * @return the new {@link DistributionSet}s
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSet#getModules()} or
     *             {@link DistributionSet#getType()})
     * @throws DistributionSetCreationFailedMissingMandatoryModuleException
     *             is {@link DistributionSet} does not contain mandatory
     *             {@link SoftwareModule}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSet> createDistributionSets(@NotNull Collection<DistributionSetCreate> creates);

    /**
     * Creates new {@link DistributionSetType}.
     *
     * @param create
     *            to create
     * @return created entity
     * 
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSetType#getMandatoryModuleTypes()} or
     *             {@link DistributionSetType#getOptionalModuleTypes()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSetType createDistributionSetType(@NotNull DistributionSetTypeCreate create);

    /**
     * Creates multiple {@link DistributionSetType}s.
     *
     * @param creates
     *            to create
     * @return created entity
     * 
     * @throws EntityNotFoundException
     *             if a provided linked entity does not exists (
     *             {@link DistributionSetType#getMandatoryModuleTypes()} or
     *             {@link DistributionSetType#getOptionalModuleTypes()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSetType> createDistributionSetTypes(@NotNull Collection<DistributionSetTypeCreate> creates);

    /**
     * <p>
     * {@link DistributionSet} can be deleted/erased from the repository if they
     * have never been assigned to any {@link Action} or {@link Target}.
     * </p>
     *
     * <p>
     * If they have been assigned that need to be marked as deleted which as a
     * result means that they cannot be assigned anymore to any targets. (define
     * e.g. findByDeletedFalse())
     * </p>
     * 
     * @param setId
     *            to delete
     *
     * @throws EntityNotFoundException
     *             if given {@link DistributionSet} does not exist
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSet(@NotNull Long setId);

    /**
     * Deleted {@link DistributionSet}s by their IDs. That is either a soft
     * delete of the entities have been linked to an {@link Action} before or a
     * hard delete if not.
     *
     * @param dsIds
     *            to be deleted
     * 
     * @throws EntityNotFoundException
     *             if (at least one) given distribution set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSet(@NotEmpty Collection<Long> dsIds);

    /**
     * deletes a distribution set meta data entry.
     *
     * @param dsId
     *            where meta data has to be deleted
     * @param key
     *            of the meta data element
     * 
     * @throws EntityNotFoundException
     *             if given set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteDistributionSetMetadata(@NotNull final Long dsId, @NotEmpty final String key);

    /**
     * Deletes or mark as delete in case the type is in use.
     *
     * @param typeId
     *            to delete
     * 
     * @throws EntityNotFoundException
     *             if given set does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSetType(@NotNull Long typeId);

    /**
     * retrieves the distribution set for a given action.
     *
     * @param actionId
     *            the action associated with the distribution set
     * @return the distribution set which is associated with the action
     * 
     * @throws EntityNotFoundException
     *             if action with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findDistributionSetByAction(@NotNull Long actionId);

    /**
     * Find {@link DistributionSet} based on given ID without details, e.g.
     * {@link DistributionSet#getModules()}.
     *
     * @param distid
     *            to look for.
     * @return {@link DistributionSet}
     * 
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findDistributionSetById(@NotNull Long distid);

    /**
     * Find {@link DistributionSet} based on given ID including (lazy loaded)
     * details, e.g. {@link DistributionSet#getModules()}.
     *
     * Note: for performance reasons it is recommended to use
     * {@link #findDistributionSetById(Long)} if details are not necessary.
     *
     * @param distid
     *            to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findDistributionSetByIdWithDetails(@NotNull Long distid);

    /**
     * Find distribution set by name and version.
     *
     * @param distributionName
     *            name of {@link DistributionSet}; case insensitive
     * @param version
     *            version of {@link DistributionSet}
     * @return the page with the found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSet> findDistributionSetByNameAndVersion(@NotEmpty String distributionName,
            @NotEmpty String version);

    /**
     * finds all meta data by the given distribution set id.
     *
     * @param distributionSetId
     *            the distribution set id to retrieve the meta data from
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     * 
     * @throws EntityNotFoundException
     *             if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(@NotNull Long distributionSetId,
            @NotNull Pageable pageable);

    /**
     * finds all meta data by the given distribution set id.
     *
     * @param distributionSetId
     *            the distribution set id to retrieve the meta data from
     * @param rsqlParam
     *            rsql query string
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     * 
     * @throws EntityNotFoundException
     *             of distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(@NotNull Long distributionSetId,
            @NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * finds all {@link DistributionSet}s.
     *
     * @param pageReq
     *            the pagination parameter
     * @param deleted
     *            if TRUE, {@link DistributionSet}s marked as deleted are
     *            returned. If FALSE, on {@link DistributionSet}s with
     *            {@link DistributionSet#isDeleted()} == FALSE are returned.
     *            <code>null</code> if both are to be returned
     * @param complete
     *            to <code>true</code> for returning only completed distribution
     *            sets or <code>false</code> for only incomplete ones nor
     *            <code>null</code> to return both.
     *
     *
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsByDeletedAndOrCompleted(@NotNull Pageable pageReq, Boolean deleted,
            Boolean complete);

    /**
     * finds all {@link DistributionSet}s.
     *
     * @param rsqlParam
     *            rsql query string
     * @param pageReq
     *            the pagination parameter
     * @param deleted
     *            if TRUE, {@link DistributionSet}s marked as deleted are
     *            returned. If FALSE, on {@link DistributionSet}s not marked as
     *            deleted are returned. <code>null</code> if both are to be
     *            returned
     * @return all found {@link DistributionSet}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsAll(@NotNull String rsqlParam, @NotNull Pageable pageReq,
            Boolean deleted);

    /**
     * finds all {@link DistributionSet}s.
     *
     * @param pageReq
     *            the pagination parameter
     * @param deleted
     *            if TRUE, {@link DistributionSet}s marked as deleted are
     *            returned. If FALSE, on {@link DistributionSet}s not marked as
     *            deleted are returned. <code>null</code> if both are to be
     *            returned
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsAll(@NotNull Pageable pageReq, Boolean deleted);

    /**
     * method retrieves all {@link DistributionSet}s from the repository in the
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
     * @param distributionSetFilterBuilder
     *            has details of filters to be applied
     * @param assignedOrInstalled
     *            the id of the Target to be ordered by
     * @return {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsAllOrderedByLinkTarget(@NotNull Pageable pageable,
            @NotNull DistributionSetFilterBuilder distributionSetFilterBuilder, @NotEmpty String assignedOrInstalled);

    /**
     * retrieves {@link DistributionSet}s by filtering on the given parameters.
     *
     * @param pageable
     *            page parameter
     * @param distributionSetFilter
     *            has details of filters to be applied.
     * @return the page of found {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsByFilters(@NotNull Pageable pageable,
            @NotNull DistributionSetFilter distributionSetFilter);

    /**
     * retrieves {@link DistributionSet}s by filtering on the given parameters.
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
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     * 
     * @throws EntityNotFoundException
     *             of distribution set tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsByTag(@NotNull final Pageable pageable, @NotNull final Long tagId);

    /**
     * retrieves {@link DistributionSet}s by filtering on the given parameters.
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
    Page<DistributionSet> findDistributionSetsByTag(@NotNull final Pageable pageable, @NotNull String rsqlParam,
            @NotNull final Long tagId);

    /**
     * @param id
     *            as {@link DistributionSetType#getId()}
     * @return {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeById(@NotNull Long id);

    /**
     * @param key
     *            as {@link DistributionSetType#getKey()}
     * @return {@link DistributionSetType}
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeByKey(@NotEmpty String key);

    /**
     * @param name
     *            as {@link DistributionSetType#getName()}
     * @return {@link DistributionSetType}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetType> findDistributionSetTypeByName(@NotEmpty String name);

    /**
     * @param pageable
     *            parameter
     * @return all {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetType> findDistributionSetTypesAll(@NotNull Pageable pageable);

    /**
     * Generic predicate based query for {@link DistributionSetType}.
     *
     * @param rsqlParam
     *            rsql query string
     * @param pageable
     *            parameter for paging
     *
     * @return the found {@link SoftwareModuleType}s
     * 
     * @throws RSQLParameterUnsupportedFieldException
     *             if a field in the RSQL string is used but not provided by the
     *             given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException
     *             if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetType> findDistributionSetTypesAll(@NotNull String rsqlParam, @NotNull Pageable pageable);

    /**
     * finds a single distribution set meta data by its id.
     *
     * @param setId
     *            of the {@link DistributionSet}
     * @param key
     *            of the meta data element
     * @return the found DistributionSetMetadata
     * 
     * @throws EntityNotFoundException
     *             is set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetMetadata> findDistributionSetMetadata(@NotNull Long setId, @NotEmpty String key);

    /**
     * Checks if a {@link DistributionSet} is currently in use by a target in
     * the repository.
     *
     * @param setId
     *            to check
     * 
     * @return <code>true</code> if in use
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean isDistributionSetInUse(@NotNull Long setId);

    /**
     * Toggles {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}s by means that if some (or all) of the targets in
     * the list have the {@link Tag} not yet assigned, they will be. If all of
     * theme have the tag already assigned they will be removed instead.
     *
     * @param dsIds
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
    DistributionSetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<Long> dsIds, @NotNull String tagName);

    /**
     * Unassigns a {@link SoftwareModule} form an existing
     * {@link DistributionSet}.
     *
     * @param setId
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
    DistributionSet unassignSoftwareModule(@NotNull Long setId, @NotNull Long moduleId);

    /**
     * Unassign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}.
     *
     * @param dsId
     *            to unassign for
     * @param tagId
     *            to unassign
     * @return the unassigned ds or <null> if no ds is unassigned
     * 
     * @throws EntityNotFoundException
     *             if set or tag with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unAssignTag(@NotNull Long dsId, @NotNull Long tagId);

    /**
     * Updates existing {@link DistributionSet}.
     *
     * @param update
     *            to update
     * 
     * @return the saved entity.
     * 
     * @throws EntityNotFoundException
     *             if given set does not exist
     * @throws EntityReadOnlyException
     *             if user tries to change requiredMigrationStep or type on a DS
     *             that is already assigned to targets
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet updateDistributionSet(@NotNull DistributionSetUpdate update);

    /**
     * updates a distribution set meta data value if corresponding entry exists.
     *
     * @param dsId
     *            {@link DistributionSet} of the meta data entry to be updated
     * @param md
     *            meta data entry to be updated
     * @return the updated meta data entry
     * 
     * @throws EntityNotFoundException
     *             in case the meta data entry does not exists and cannot be
     *             updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetMetadata updateDistributionSetMetadata(@NotNull Long dsId, @NotNull MetaData md);

    /**
     * Updates existing {@link DistributionSetType}. Resets assigned
     * {@link SoftwareModuleType}s as well and sets as provided.
     *
     * @param update
     *            to update
     * 
     * @return updated entity
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} does not exists and
     *             cannot be updated
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} is already in use by a
     *             {@link DistributionSet} and user tries to change list of
     *             {@link SoftwareModuleType}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType updateDistributionSetType(@NotNull DistributionSetTypeUpdate update);

    /**
     * Unassigns a {@link SoftwareModuleType} from the
     * {@link DistributionSetType}. Does nothing if {@link SoftwareModuleType}
     * has not been assigned in the first place.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleId
     *            to unassign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} does not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType unassignSoftwareModuleType(@NotNull Long dsTypeId, @NotNull Long softwareModuleId);

    /**
     * Assigns {@link DistributionSetType#getMandatoryModuleTypes()}.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleTypeIds
     *            to assign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} or at least one of
     *             the {@link SoftwareModuleType}s do not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignOptionalSoftwareModuleTypes(@NotNull Long dsTypeId,
            @NotEmpty Collection<Long> softwareModuleTypeIds);

    /**
     * Assigns {@link DistributionSetType#getOptionalModuleTypes()}.
     * 
     * @param dsTypeId
     *            to update
     * @param softwareModuleTypes
     *            to assign
     * @return updated {@link DistributionSetType}
     * 
     * @throws EntityNotFoundException
     *             in case the {@link DistributionSetType} or at least one of
     *             the {@link SoftwareModuleType}s do not exist
     * 
     * @throws EntityReadOnlyException
     *             if the {@link DistributionSetType} while it is already in use
     *             by a {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType assignMandatorySoftwareModuleTypes(@NotNull Long dsTypeId,
            @NotEmpty Collection<Long> softwareModuleTypes);

    /**
     * Retrieves all distribution set without details.
     *
     * @param ids
     *            the ids to for
     * @return the found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<DistributionSet> findDistributionSetsById(@NotEmpty Collection<Long> ids);

}
