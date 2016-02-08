/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.eventbus.event.DistributionSetTagAssigmentResultEvent;
import org.eclipse.hawkbit.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.exception.DistributionSetCreationFailedMissingMandatoryModuleException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityLockedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata_;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSetTypeElement;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.specifications.DistributionSetTypeSpecification;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * Business facade for managing the {@link DistributionSet}s.
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class DistributionSetManagement {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    private DistributionSetMetadataRepository distributionSetMetadataRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

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
    public DistributionSet findDistributionSetByIdWithDetails(@NotNull final Long distid) {
        return distributionSetRepository.findOne(DistributionSetSpecification.byId(distid));
    }

    /**
     * Find {@link DistributionSet} based on given ID without details, e.g.
     * {@link DistributionSet#getAgentHub()}.
     *
     * @param distid
     *            to look for.
     * @return {@link DistributionSet} or <code>null</code> if it does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSet findDistributionSetById(@NotNull final Long distid) {
        return distributionSetRepository.findOne(distid);
    }

    /**
     * {@link Entity} based method call for
     * {@link #toggleTagAssignment(Collection, String)}.
     *
     * @param sets
     *            to toggle for
     * @param tag
     *            to toogle
     * @return {@link DistributionSetTagAssigmentResult} with all metadata of
     *         the assignment outcome.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetTagAssigmentResult toggleTagAssignment(@NotEmpty final List<DistributionSet> sets,
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
     * @return {@link DistributionSetTagAssigmentResult} with all metadata of
     *         the assignment outcome.
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetTagAssigmentResult toggleTagAssignment(@NotEmpty final Collection<Long> dsIds,
            @NotNull final String tagName) {

        final Iterable<DistributionSet> sets = findDistributionSetListWithDetails(dsIds);
        final DistributionSetTag myTag = tagManagement.findDistributionSetTag(tagName);

        DistributionSetTagAssigmentResult result = null;
        final List<DistributionSet> allDSs = new ArrayList<>();
        for (final DistributionSet set : sets) {
            if (set.getTags().add(myTag)) {
                allDSs.add(set);
            }
        }

        // unassigment case
        if (allDSs.isEmpty()) {
            for (final DistributionSet set : sets) {
                if (set.getTags().remove(myTag)) {
                    allDSs.add(set);
                }
            }
            result = new DistributionSetTagAssigmentResult(dsIds.size() - allDSs.size(), 0, allDSs.size(),
                    Collections.emptyList(), distributionSetRepository.save(allDSs), myTag);
        } else {
            result = new DistributionSetTagAssigmentResult(dsIds.size() - allDSs.size(), allDSs.size(), 0,
                    distributionSetRepository.save(allDSs), Collections.emptyList(), myTag);
        }

        final DistributionSetTagAssigmentResult resultAssignment = result;
        afterCommit.afterCommit(() -> eventBus.post(new DistributionSetTagAssigmentResultEvent(resultAssignment)));

        // no reason to persist the tag
        entityManager.detach(myTag);
        return result;
    }

    /**
     * Retrieves {@link DistributionSet} List including details information,
     * i.e. @link BaseSoftwareModule}s and {@link DistributionSetTag}s.
     *
     * @param distributionIdSet
     *            List of {@link DistributionSet} IDs to be found
     * @return the found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<DistributionSet> findDistributionSetListWithDetails(
            @NotEmpty final Collection<Long> distributionIdSet) {
        return distributionSetRepository.findAll(DistributionSetSpecification.byIds(distributionIdSet));
    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSet updateDistributionSet(@NotNull final DistributionSet ds) {
        checkNotNull(ds.getId());
        final DistributionSet persisted = findDistributionSetByIdWithDetails(ds.getId());
        checkDistributionSetSoftwareModulesIsAllowedToModify(ds, persisted.getModules());
        return distributionSetRepository.save(ds);
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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteDistributionSet(@NotNull final DistributionSet set) {
        this.deleteDistributionSet(set.getId());
    }

    /**
     * Deleted {@link DistributionSet}s by their IDs. That is either a soft
     * delete of the entities have been linked to an {@link UpdateAction} before
     * or a hard delete if not.
     *
     * @param distributionSetIDs
     *            to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteDistributionSet(@NotEmpty final Long... distributionSetIDs) {
        final List<Long> toHardDelete = new ArrayList<>();

        final List<Long> assigned = distributionSetRepository.findAssignedDistributionSetsById(distributionSetIDs);

        // soft delete assigned
        if (!assigned.isEmpty()) {
            distributionSetRepository.deleteDistributionSet(assigned.toArray(new Long[assigned.size()]));
        }

        // mark the rest as hard delete
        for (final Long setId : distributionSetIDs) {
            if (!assigned.contains(setId)) {
                toHardDelete.add(setId);
            }
        }

        // hard delete the rest if exixts
        if (!toHardDelete.isEmpty()) {
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list,
            // see MECS-403
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }
    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public DistributionSet createDistributionSet(@NotNull final DistributionSet dSet) {
        prepareDsSave(dSet);
        if (dSet.getType() == null) {
            dSet.setType(systemManagement.getTenantMetadata().getDefaultDsType());
        }
        return distributionSetRepository.save(dSet);
    }

    private void prepareDsSave(final DistributionSet dSet) {
        if (dSet.getId() != null) {
            throw new EntityAlreadyExistsException("Parameter seems to be an existing, already persisted entity");
        }

        if (distributionSetRepository.countByNameAndVersion(dSet.getName(), dSet.getVersion()) > 0) {
            throw new EntityAlreadyExistsException("DistributionSet with that name and version already exists.");
        }

    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<DistributionSet> createDistributionSets(@NotNull final Iterable<DistributionSet> distributionSets) {
        for (final DistributionSet ds : distributionSets) {
            prepareDsSave(ds);
        }
        return distributionSetRepository.save(distributionSets);
    }

    /**
     * Assigns {@link SoftwareModule} to existing {@link DistributionSet}.
     *
     * @param ds
     *            to assign and update
     * @param softwareModules
     *            to get assigned
     * @return the updated {@link DistributionSet}.
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSet assignSoftwareModules(@NotNull final DistributionSet ds,
            final Set<SoftwareModule> softwareModules) {
        checkDistributionSetSoftwareModulesIsAllowedToModify(ds, softwareModules);
        for (final SoftwareModule softwareModule : softwareModules) {
            ds.addModule(softwareModule);
        }
        return distributionSetRepository.save(ds);
    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSet unassignSoftwareModule(@NotNull final DistributionSet ds,
            final SoftwareModule softwareModule) {
        final Set<SoftwareModule> softwareModules = new HashSet<>();
        softwareModules.add(softwareModule);
        ds.removeModule(softwareModule);
        checkDistributionSetSoftwareModulesIsAllowedToModify(ds, softwareModules);
        return distributionSetRepository.save(ds);
    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetType updateDistributionSetType(@NotNull final DistributionSetType dsType) {
        checkNotNull(dsType.getId());

        final DistributionSetType persisted = distributionSetTypeRepository.findOne(dsType.getId());

        // throw exception if user tries to update a DS type that is already in
        // use
        if (!persisted.areModuleEntriesIdentical(dsType) && distributionSetRepository.countByType(persisted) > 0) {
            throw new EntityReadOnlyException(
                    String.format("distribution set type %s set is already assigned to targets and cannot be changed",
                            dsType.getName()));
        }

        return distributionSetTypeRepository.save(dsType);
    }

    /**
     * Generic predicate based query for {@link DistributionSetType}.
     *
     * @param spec
     *            of the search
     * @param pageable
     *            parametsr for paging
     *
     * @return the found {@link SoftwareModuleType}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<DistributionSetType> findDistributionSetTypesByPredicate(
            @NotNull final Specification<DistributionSetType> spec, @NotNull final Pageable pageable) {
        return distributionSetTypeRepository.findAll(spec, pageable);
    }

    /**
     * @param pageable
     *            parameter
     * @return all {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<DistributionSetType> findDistributionSetTypesAll(@NotNull final Pageable pageable) {
        return distributionSetTypeRepository.findByDeleted(pageable, false);
    }

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
    public Page<DistributionSet> findDistributionSetsByFilters(@NotNull final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<DistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        return findByCriteriaAPI(pageable, specList);
    }

    /**
     *
     * @param distributionSetFilter
     *            had details of filters to be applied
     * @return a single DistributionSet which is either installed or assigned to
     *         a specific target or {@code null}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    private DistributionSet findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<DistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        if (!specList.isEmpty()) {
            Specifications<DistributionSet> specs = Specifications.where(specList.get(0));
            specList.remove(0);
            for (final Specification<DistributionSet> s : specList) {
                specs = specs.and(s);
            }
            return distributionSetRepository.findOne(specs);
        }
        return null;
    }

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
     *            <code>null</code> to return both..
     * @param complete
     *            set to if <code>false</code> uncomplete DS should also be
     *            shown.
     *
     *
     * @return all found {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<DistributionSet> findDistributionSetsAll(@NotNull final Pageable pageReq, final Boolean deleted,
            final Boolean complete) {
        final List<Specification<DistributionSet>> specList = new ArrayList<>();

        if (deleted != null) {
            final Specification<DistributionSet> spec = DistributionSetSpecification.isDeleted(deleted);
            specList.add(spec);
        }

        if (complete != null) {
            final Specification<DistributionSet> spec = DistributionSetSpecification.isCompleted(complete);
            specList.add(spec);
        }

        return findByCriteriaAPI(pageReq, specList);
    }

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
    public Page<DistributionSet> findDistributionSetsAll(@NotNull final Specification<DistributionSet> spec,
            @NotNull final Pageable pageReq, final Boolean deleted) {
        final List<Specification<DistributionSet>> specList = new ArrayList<>();
        if (deleted != null) {
            specList.add(DistributionSetSpecification.isDeleted(deleted));
        }
        specList.add(spec);
        return findByCriteriaAPI(pageReq, specList);
    }

    /**
     * method retrieves all {@link DistributionSet}s from the repo in the
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
     *            the ID of the Target to be ordered by
     * @return
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<DistributionSet> findDistributionSetsAllOrderedByLinkTarget(@NotNull final Pageable pageable,
            @NotNull final DistributionSetFilterBuilder distributionSetFilterBuilder,
            @NotNull final String assignedOrInstalled) {

        final DistributionSetFilter filterWithInstalledTargets = distributionSetFilterBuilder
                .setInstalledTargetId(assignedOrInstalled).setAssignedTargetId(null).build();
        final DistributionSet installedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithInstalledTargets);

        final DistributionSetFilter filterWithAssignedTargets = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(assignedOrInstalled).build();
        final DistributionSet assignedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithAssignedTargets);

        final DistributionSetFilter dsFilterWithNoTargetLinked = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(null).build();
        // first fine the distribution sets filtered by the given filter
        // parameters
        final Page<DistributionSet> findDistributionSetsByFilters = findDistributionSetsByFilters(pageable,
                dsFilterWithNoTargetLinked);

        final List<DistributionSet> resultSet = new LinkedList<>(findDistributionSetsByFilters.getContent());
        int orderIndex = 0;
        if (installedDS != null) {
            final boolean remove = resultSet.remove(installedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, installedDS);
            orderIndex++;
        }
        if (assignedDS != null && !assignedDS.equals(installedDS)) {
            final boolean remove = resultSet.remove(assignedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, assignedDS);
        }

        return new PageImpl<>(resultSet, pageable, findDistributionSetsByFilters.getTotalElements());
    }

    private Long countDistributionSetByCriteriaAPI(@NotEmpty final List<Specification<DistributionSet>> specList) {
        Specifications<DistributionSet> specs = Specifications.where(specList.get(0));
        if (specList.size() > 1) {
            for (final Specification<DistributionSet> s : specList.subList(1, specList.size())) {
                specs = specs.and(s);
            }
        }

        return distributionSetRepository.count(specs);
    }

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
    public DistributionSet findDistributionSetByNameAndVersion(@NotEmpty final String distributionName,
            @NotEmpty final String version) {
        final Specification<DistributionSet> spec = DistributionSetSpecification
                .equalsNameAndVersionIgnoreCase(distributionName, version);
        return distributionSetRepository.findOne(spec);

    }

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
    public Iterable<DistributionSet> findDistributionSetList(@NotEmpty final Collection<Long> dist) {
        return distributionSetRepository.findAll(dist);
    }

    /**
     * Count all {@link DistributionSet}s in the repository that are not marked
     * as deleted.
     *
     * @return number of {@link DistributionSet}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countDistributionSetsAll() {

        final List<Specification<DistributionSet>> specList = new ArrayList<>();

        final Specification<DistributionSet> spec = DistributionSetSpecification.isDeleted(Boolean.FALSE);
        specList.add(spec);

        return countDistributionSetByCriteriaAPI(specList);
    }

    /**
     * @return number of {@link DistributionSetType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countDistributionSetTypesAll() {
        return distributionSetTypeRepository.countByDeleted(false);
    }

    /**
     * @param name
     *            as {@link DistributionSetType#getName()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSetType findDistributionSetTypeByName(@NotNull final String name) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name));
    }

    /**
     * @param id
     *            as {@link DistributionSetType#getId()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSetType findDistributionSetTypeById(@NotNull final Long id) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byId(id));
    }

    /**
     * @param key
     *            as {@link DistributionSetType#getKey()}
     * @return {@link DistributionSetType} if found or <code>null</code> if not
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSetType findDistributionSetTypeByKey(@NotNull final String key) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key));
    }

    /**
     * Creates new {@link DistributionSetType}.
     *
     * @param type
     *            to create
     * @return created {@link Entity}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public DistributionSetType createDistributionSetType(@NotNull final DistributionSetType type) {
        if (type.getId() != null) {
            throw new EntityAlreadyExistsException("Given type contains an Id!");
        }

        return distributionSetTypeRepository.save(type);
    }

    /**
     * Deletes or markes as delete in case the type is in use.
     *
     * @param type
     *            to delete
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteDistributionSetType(@NotNull final DistributionSetType type) {

        if (distributionSetRepository.countByType(type) > 0) {
            final DistributionSetType toDelete = entityManager.merge(type);
            toDelete.setDeleted(true);
            distributionSetTypeRepository.save(toDelete);
        } else {
            distributionSetTypeRepository.delete(type.getId());
        }
    }

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
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetMetadata createDistributionSetMetadata(@NotNull final DistributionSetMetadata metadata) {
        if (distributionSetMetadataRepository.exists(metadata.getId())) {
            throwMetadataKeyAlreadyExists(metadata.getId().getKey());
        }
        // merge base software module so optLockRevision gets updated and audit
        // log written because
        // modifying metadata is modifying the base distribution set itself for
        // auditing purposes.
        entityManager.merge(metadata.getDistributionSet()).setLastModifiedAt(0L);
        return distributionSetMetadataRepository.save(metadata);
    }

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
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public List<DistributionSetMetadata> createDistributionSetMetadata(
            @NotEmpty final Collection<DistributionSetMetadata> metadata) {
        for (final DistributionSetMetadata distributionSetMetadata : metadata) {
            checkAndThrowAlreadyIfDistributionSetMetadataExists(distributionSetMetadata.getId());
        }
        metadata.forEach(m -> entityManager.merge(m.getDistributionSet()).setLastModifiedAt(-1L));
        return (List<DistributionSetMetadata>) distributionSetMetadataRepository.save(metadata);
    }

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
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public DistributionSetMetadata updateDistributionSetMetadata(@NotNull final DistributionSetMetadata metadata) {
        // check if exists otherwise throw entity not found exception
        findOne(metadata.getId());
        // touch it to update the lock revision because we are modifying the
        // DS indirectly
        entityManager.merge(metadata.getDistributionSet()).setLastModifiedAt(0L);
        return distributionSetMetadataRepository.save(metadata);
    }

    /**
     * deletes a distribution set meta data entry.
     *
     * @param id
     *            the ID of the distribution set meta data to delete
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public void deleteDistributionSetMetadata(@NotNull final DsMetadataCompositeKey id) {
        distributionSetMetadataRepository.delete(id);
    }

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
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(
            @NotNull final Long distributionSetId, @NotNull final Pageable pageable) {

        return distributionSetMetadataRepository.findAll(new Specification<DistributionSetMetadata>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetMetadata> root, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return cb.equal(root.get(DistributionSetMetadata_.distributionSet).get(DistributionSet_.id),
                        distributionSetId);
            }
        }, pageable);

    }

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
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(
            @NotNull final Long distributionSetId, @NotNull final Specification<DistributionSetMetadata> spec,
            @NotNull final Pageable pageable) {
        return distributionSetMetadataRepository.findAll(new Specification<DistributionSetMetadata>() {
            @Override
            public Predicate toPredicate(final Root<DistributionSetMetadata> root, final CriteriaQuery<?> query,
                    final CriteriaBuilder cb) {
                return cb.and(cb.equal(root.get(DistributionSetMetadata_.distributionSet).get(DistributionSet_.id),
                        distributionSetId), spec.toPredicate(root, query, cb));
            }
        }, pageable);
    }

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
    public DistributionSetMetadata findOne(@NotNull final DsMetadataCompositeKey id) {
        final DistributionSetMetadata findOne = distributionSetMetadataRepository.findOne(id);
        if (findOne == null) {
            throw new EntityNotFoundException("Metadata with key '" + id.getKey() + "' does not exist");
        }
        return findOne;
    }

    /**
     * retrieves the distribution set for a given action.
     *
     * @param action
     *            the action associated with the distribution set
     * @return the distribution set which is associated with the action
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public DistributionSet findDistributionSetByAction(@NotNull final Action action) {
        return distributionSetRepository.findByAction(action);
    }

    /**
     * Creates multiple {@link DistributionSetType}s.
     *
     * @param types
     *            to create
     * @return created {@link Entity}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<DistributionSetType> createDistributionSetTypes(@NotNull final Collection<DistributionSetType> types) {
        return types.stream().map(type -> createDistributionSetType(type)).collect(Collectors.toList());
    }

    /**
     * Checking Distribution Set is already using while assign Software module.
     * 
     * @param distributionSet
     * @param softwareModules
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public void checkDistributionSetAlreadyUse(final DistributionSet distributionSet) {
        checkDistributionSetSoftwareModulesIsAllowedToModify(distributionSet);
    }

    private List<Specification<DistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<DistributionSet>> specList = new ArrayList<>();

        Specification<DistributionSet> spec;

        if (null != distributionSetFilter.getIsComplete()) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (null != distributionSetFilter.getIsDeleted()) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
            specList.add(spec);
        }

        if (distributionSetFilter.getType() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getType());
            specList.add(spec);
        }

        if (!Strings.isNullOrEmpty(distributionSetFilter.getSearchText())) {
            spec = DistributionSetSpecification.likeNameOrDescriptionOrVersion(distributionSetFilter.getSearchText());
            specList.add(spec);
        }

        if (isDSWithNoTagSelected(distributionSetFilter) || isTagsSelected(distributionSetFilter)) {
            spec = DistributionSetSpecification.hasTags(distributionSetFilter.getTagNames(),
                    distributionSetFilter.getSelectDSWithNoTag());
            specList.add(spec);
        }
        if (distributionSetFilter.getInstalledTargetId() != null) {
            spec = DistributionSetSpecification.installedTarget(distributionSetFilter.getInstalledTargetId());
            specList.add(spec);
        }
        if (distributionSetFilter.getAssignedTargetId() != null) {
            spec = DistributionSetSpecification.assignedTarget(distributionSetFilter.getAssignedTargetId());
            specList.add(spec);
        }
        return specList;
    }

    private void checkDistributionSetSoftwareModulesIsAllowedToModify(final DistributionSet distributionSet,
            final Set<SoftwareModule> softwareModules) {
        if (!new HashSet<SoftwareModule>(distributionSet.getModules()).equals(softwareModules)
                && actionRepository.countByDistributionSet(distributionSet) > 0) {
            throw new EntityLockedException(
                    String.format("distribution set %s:%s is already assigned to targets and cannot be changed",
                            distributionSet.getName(), distributionSet.getVersion()));
        }
    }

    private void checkDistributionSetSoftwareModulesIsAllowedToModify(final DistributionSet distributionSet) {
        if (actionRepository.countByDistributionSet(distributionSet) > 0) {
            throw new EntityLockedException(
                    String.format("distribution set %s:%s is already assigned to targets and cannot be changed",
                            distributionSet.getName(), distributionSet.getVersion()));
        }
    }

    private Boolean isDSWithNoTagSelected(final DistributionSetFilter distributionSetFilter) {
        if (distributionSetFilter.getSelectDSWithNoTag() != null && distributionSetFilter.getSelectDSWithNoTag()) {
            return true;
        }
        return false;
    }

    private Boolean isTagsSelected(final DistributionSetFilter distributionSetFilter) {
        if (distributionSetFilter.getTagNames() != null && !distributionSetFilter.getTagNames().isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * executes findAll with the given {@link DistributionSet}
     * {@link Specification}s.
     *
     * @param pageable
     *            paging parameter
     * @param specList
     *            list of @link {@link Specification}
     * @return the page with the found {@link DistributionSet}
     */
    private Page<DistributionSet> findByCriteriaAPI(@NotNull final Pageable pageable,
            final List<Specification<DistributionSet>> specList) {
        Specifications<DistributionSet> specs = null;
        if (!specList.isEmpty()) {
            specs = Specifications.where(specList.get(0));
        }
        if (specList.size() > 1) {
            for (final Specification<DistributionSet> s : specList.subList(1, specList.size())) {
                specs = specs.and(s);
            }
        }

        if (specs == null) {
            return distributionSetRepository.findAll(pageable);
        } else {
            return distributionSetRepository.findAll(specs, pageable);
        }
    }

    private void checkAndThrowAlreadyIfDistributionSetMetadataExists(final DsMetadataCompositeKey metadataId) {
        if (distributionSetMetadataRepository.exists(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    private void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

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
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public List<DistributionSet> assignTag(@NotEmpty final Collection<Long> dsIds,
            @NotNull final DistributionSetTag tag) {
        final List<DistributionSet> allDs = findDistributionSetListWithDetails(dsIds);

        allDs.forEach(ds -> ds.getTags().add(tag));
        final List<DistributionSet> save = distributionSetRepository.save(allDs);

        afterCommit.afterCommit(() -> {

            final DistributionSetTagAssigmentResult result = new DistributionSetTagAssigmentResult(0, save.size(), 0,
                    save, Collections.emptyList(), tag);
            eventBus.post(new DistributionSetTagAssigmentResultEvent(result));
        });

        return save;
    }

    /**
     * Unassign all {@link DistributionSet} from a given
     * {@link DistributionSetTag} .
     *
     * @param tag
     *            to unassign all ds
     * @return list of unassigned ds
     */
    @Modifying
    @Transactional
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public List<DistributionSet> unAssignAllDistributionSetsByTag(@NotNull final DistributionSetTag tag) {
        return unAssignTag(tag.getAssignedToDistributionSet(), tag);
    }

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
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    public DistributionSet unAssignTag(@NotNull final Long dsId, @NotNull final DistributionSetTag distributionSetTag) {
        final List<DistributionSet> allDs = findDistributionSetListWithDetails(Arrays.asList(dsId));
        final List<DistributionSet> unAssignTag = unAssignTag(allDs, distributionSetTag);
        return unAssignTag.isEmpty() ? null : unAssignTag.get(0);
    }

    private List<DistributionSet> unAssignTag(final Collection<DistributionSet> distributionSets,
            final DistributionSetTag tag) {
        distributionSets.forEach(ds -> ds.getTags().remove(tag));
        return distributionSetRepository.save(distributionSets);
    }
}
