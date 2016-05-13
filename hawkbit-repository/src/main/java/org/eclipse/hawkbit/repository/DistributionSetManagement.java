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
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.exception.DistributionSetCreationFailedMissingMandatoryModuleException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSetTypeElement;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface DistributionSetManagement {

    // TODO rename/document the whole with details thing (document what the
    // details are and maybe find a better name, e.g. with dependencies?)

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param ds
     *            to assign and update
     * @param softwareModules
     *            to get assigned
     * @return the updated {@link DistributionSet}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet assignSoftwareModules(@NotNull DistributionSet ds, Set<SoftwareModule> softwareModules);

    /**
     * Assign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}s.
     *
     * @param dsIds
     *            to assign for
     * @param tag
     *            to assign
     * @return list of assigned ds
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> assignTag(@NotEmpty Collection<Long> dsIds, @NotNull DistributionSetTag tag);

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     *
     * @return number of {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetsAll();

    /**
     * @return number of {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Long countDistributionSetTypesAll();

    /**
     * Creates a new {@link DistributionSet}.
     *
     * @param dSet
     *            {@link DistributionSet} to be created
     * @return the new persisted {@link DistributionSet}
     *
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws DistributionSetCreationFailedMissingMandatoryModuleException
     *             is {@link DistributionSet} does not contain mandatory
     *             {@link SoftwareModule}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSet createDistributionSet(@NotNull DistributionSet dSet);

    /**
     * creates a list of distribution set meta data entries.
     *
     * @param metadata
     *            the meta data entries to create or update
     * @return the updated or created distribution set meta data entries
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSetMetadata> createDistributionSetMetadata(@NotEmpty Collection<DistributionSetMetadata> metadata);

    /**
     * creates or updates a single distribution set meta data entry.
     *
     * @param metadata
     *            the meta data entry to create or update
     * @return the updated or created distribution set meta data entry
     * @throws EntityAlreadyExistsException
     *             in case the meta data entry already exists for the specific
     *             key
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetMetadata createDistributionSetMetadata(@NotNull DistributionSetMetadata metadata);

    /**
     * Creates multiple {@link DistributionSet}s.
     *
     * @param distributionSets
     *            to be created
     * @return the new {@link DistributionSet}s
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     * @throws DistributionSetCreationFailedMissingMandatoryModuleException
     *             is {@link DistributionSet} does not contain mandatory
     *             {@link SoftwareModule}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    List<DistributionSet> createDistributionSets(@NotNull Collection<DistributionSet> distributionSets);

    /**
     * Creates new {@link DistributionSetType}.
     *
     * @param type
     *            to create
     * @return created {@link Entity}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    DistributionSetType createDistributionSetType(@NotNull DistributionSetType type);

    /**
     * Creates multiple {@link DistributionSetType}s.
     *
     * @param types
     *            to create
     * @return created {@link Entity}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    default List<DistributionSetType> createDistributionSetTypes(@NotNull final Collection<DistributionSetType> types) {
        return types.stream().map(this::createDistributionSetType).collect(Collectors.toList());
    }

    /**
     * <p>
     * {@link DistributionSet} can be deleted/erased from the repository if they
     * have never been assigned to any {@link UpdateAction} or {@link Target}.
     * </p>
     *
     * <p>
     * If they have been assigned that need to be marked as deleted which as a
     * result means that they cannot be assigned anymore to any targets. (define
     * e.g. findByDeletedFalse())
     * </p>
     *
     * @param set
     *            to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    default void deleteDistributionSet(@NotNull final DistributionSet set) {
        deleteDistributionSet(set.getId());
    }

    /**
     * Deleted {@link DistributionSet}s by their IDs. That is either a soft
     * delete of the entities have been linked to an {@link UpdateAction} before
     * or a hard delete if not.
     *
     * @param distributionSetIDs
     *            to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSet(@NotEmpty Long... distributionSetIDs);

    /**
     * deletes a distribution set meta data entry.
     *
     * @param id
     *            the ID of the distribution set meta data to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void deleteDistributionSetMetadata(@NotNull DsMetadataCompositeKey id);

    /**
     * Deletes or mark as delete in case the type is in use.
     *
     * @param type
     *            to delete
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteDistributionSetType(@NotNull DistributionSetType type);

    /**
     * retrieves the distribution set for a given action.
     *
     * @param action
     *            the action associated with the distribution set
     * @return the distribution set which is associated with the action
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet findDistributionSetByAction(@NotNull Action action);

    /**
     * Find {@link DistributionSet} based on given ID without details, e.g.
     * {@link DistributionSet#getAgentHub()}.
     *
     * @param distid
     *            to look for.
     * @return {@link DistributionSet} or <code>null</code> if it does not exist
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    default DistributionSet findDistributionSetById(@NotNull final Long distid) {
        return findDistributionSetByIdWithDetails(distid);
    }

    /**
     * Find {@link DistributionSet} based on given ID including (lazy loaded)
     * details, e.g. {@link DistributionSet#getAgentHub()}.
     *
     * Note: for performance reasons it is recommended to use
     * {@link #findDistributionSetById(Long)} if details are not necessary.
     *
     * @param distid
     *            to look for.
     * @return {@link DistributionSet} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSet findDistributionSetByIdWithDetails(@NotNull Long distid);

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
    DistributionSet findDistributionSetByNameAndVersion(@NotEmpty String distributionName, @NotEmpty String version);

    /**
     * Retrieves {@link DistributionSet} List for overview purposes (no
     * {@link SoftwareModule}s and {@link DistributionSetTag}s).
     *
     * Please use {@link #findDistributionSetListWithDetails(Iterable)} if
     * details are required.
     *
     * @param dist
     *            List of {@link DistributionSet} IDs to be found
     * @return the found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Iterable<DistributionSet> findDistributionSetList(Collection<Long> dist);

    /**
     * Retrieves {@link DistributionSet} List including details information,
     * i.e. @link BaseSoftwareModule}s and {@link DistributionSetTag}s.
     *
     * @param distributionIdSet
     *            List of {@link DistributionSet} IDs to be found
     * @return the found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<DistributionSet> findDistributionSetListWithDetails(Collection<Long> distributionIdSet);

    /**
     * finds all meta data by the given distribution set id.
     *
     * @param distributionSetId
     *            the distribution set id to retrieve the meta data from
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(@NotNull Long distributionSetId,
            @NotNull Pageable pageable);

    /**
     * finds all meta data by the given distribution set id.
     *
     * @param distributionSetId
     *            the distribution set id to retrieve the meta data from
     * @param spec
     *            the specification to filter the result
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given distribution
     *         set id
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(@NotNull Long distributionSetId,
            @NotNull Specification<DistributionSetMetadata> spec, @NotNull Pageable pageable);

    // TODO discuss: use enum instead of the true,false,null switch ?
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
     * @param complete
     *            set to if <code>false</code> incomplete DS should also be
     *            shown.
     *
     *
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsAll(@NotNull Pageable pageReq, Boolean deleted, Boolean complete);

    /**
     * finds all {@link DistributionSet}s.
     *
     * @param spec
     *            the specification to add for the search query.
     * @param pageReq
     *            the pagination parameter
     * @param deleted
     *            if TRUE, {@link DistributionSet}s marked as deleted are
     *            returned. If FALSE, on {@link DistributionSet}s with
     *            {@link DistributionSet#isDeleted()} == FALSE are returned.
     *            <code>null</code> if both are to be returned
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSet> findDistributionSetsAll(@NotNull Specification<DistributionSet> spec,
            @NotNull Pageable pageReq, Boolean deleted);

    /**
     * method retrieves all {@link DistributionSet}s from the repository in the
     * following order:
     * <p>
     * 1) {@link DistributionSet}s which have the given {@link Target} as
     * {@link TargetStatus#getInstalledDistributionSet()}
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
     *            the controllerID of the Target to be ordered by
     * @return
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
     * @param id
     *            as {@link DistributionSetType#getId()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetType findDistributionSetTypeById(@NotNull Long id);

    /**
     * @param key
     *            as {@link DistributionSetType#getKey()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetType findDistributionSetTypeByKey(@NotNull String key);

    /**
     * @param name
     *            as {@link DistributionSetType#getName()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetType findDistributionSetTypeByName(@NotEmpty String name);

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
     * @param spec
     *            of the search
     * @param pageable
     *            parameter for paging
     *
     * @return the found {@link SoftwareModuleType}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetType> findDistributionSetTypesAll(@NotNull Specification<DistributionSetType> spec,
            @NotNull Pageable pageable);

    /**
     * finds a single distribution set meta data by its id.
     *
     * @param id
     *            the id of the distribution set meta data containing the meta
     *            data key and the ID of the distribution set
     * @return the found DistributionSetMetadata or {@code null} if not exits
     * @throws EntityNotFoundException
     *             in case the meta data does not exists for the given key
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    DistributionSetMetadata findOne(@NotNull DsMetadataCompositeKey id);

    /**
     * Checks if a {@link DistributionSet} is currently in use by a target in
     * the repository.
     *
     * @param distributionSet
     *            to check
     * 
     * @return <code>true</code> if in use
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean isDistributionSetInUse(@NotNull DistributionSet distributionSet);

    /**
     * {@link Entity} based method call for
     * {@link #toggleTagAssignment(Collection, String)}.
     *
     * @param sets
     *            to toggle for
     * @param tag
     *            to toggle
     * @return {@link DistributionSetTagAssignmentResult} with all meta data of
     *         the assignment outcome.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    default DistributionSetTagAssignmentResult toggleTagAssignment(@NotEmpty final Collection<DistributionSet> sets,
            @NotNull final DistributionSetTag tag) {
        return toggleTagAssignment(sets.stream().map(ds -> ds.getId()).collect(Collectors.toList()), tag.getName());
    }

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
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetTagAssignmentResult toggleTagAssignment(@NotEmpty Collection<Long> dsIds, @NotNull String tagName);

    /**
     * Unassign all {@link DistributionSet} from a given
     * {@link DistributionSetTag} .
     *
     * @param tag
     *            to unassign all ds
     * @return list of unassigned ds
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    List<DistributionSet> unAssignAllDistributionSetsByTag(@NotNull DistributionSetTag tag);

    /**
     * Unassigns a {@link SoftwareModule} form an existing
     * {@link DistributionSet}.
     *
     * @param ds
     *            to get unassigned form
     * @param softwareModule
     *            to get unassigned
     * @return the updated {@link DistributionSet}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unassignSoftwareModule(@NotNull DistributionSet ds, @NotNull SoftwareModule softwareModule);

    /**
     * Unassign a {@link DistributionSetTag} assignment to given
     * {@link DistributionSet}.
     *
     * @param dsId
     *            to unassign for
     * @param distributionSetTag
     *            to unassign
     * @return the unassigned ds or <null> if no ds is unassigned
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet unAssignTag(@NotNull Long dsId, @NotNull DistributionSetTag distributionSetTag);

    /**
     * Updates existing {@link DistributionSet}.
     *
     * @param ds
     *            to update
     * @return the saved {@link Entity}.
     * @throws NullPointerException
     *             of {@link DistributionSet#getId()} is <code>null</code>
     * @throw DataDependencyViolationException in case of illegal update
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSet updateDistributionSet(@NotNull DistributionSet ds);

    /**
     * updates a distribution set meta data value if corresponding entry exists.
     *
     * @param metadata
     *            the meta data entry to be updated
     * @return the updated meta data entry
     * @throws EntityNotFoundException
     *             in case the meta data entry does not exists and cannot be
     *             updated
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetMetadata updateDistributionSetMetadata(@NotNull DistributionSetMetadata metadata);

    /**
     * Updates existing {@link DistributionSetType}. However, keep in mind that
     * is not possible to change the {@link DistributionSetTypeElement}s while
     * the DS type is already in use.
     *
     * @param dsType
     *            to update
     * @return updated {@link Entity}
     *
     * @throws EntityReadOnlyException
     *             if use tries to change the {@link DistributionSetTypeElement}
     *             s while the DS type is already in use.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    DistributionSetType updateDistributionSetType(@NotNull DistributionSetType dsType);

}