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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.CustomSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSet_;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata_;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModule_;
import org.eclipse.hawkbit.repository.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.specifications.SpecificationsBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * Business facade for managing {@link SoftwareModule}s.
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class SoftwareManagement {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    private SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Autowired
    private NoCountPagingRepository criteriaNoCountDao;

    @Autowired
    private AuditorAware<String> auditorProvider;

    @Autowired
    private ArtifactManagement artifactManagement;

    /**
     * Updates existing {@link SoftwareModule}. Update-able values are
     * {@link SoftwareModule#getDescription()}
     * {@link SoftwareModule#getVendor()}.
     *
     * @param sm
     *            to update
     *
     * @return the saved {@link Entity}.
     *
     * @throws NullPointerException
     *             of {@link SoftwareModule#getId()} is <code>null</code>
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public SoftwareModule updateSoftwareModule(@NotNull final SoftwareModule sm) {
        checkNotNull(sm.getId());

        final SoftwareModule module = softwareModuleRepository.findOne(sm.getId());

        boolean updated = false;
        if (null == sm.getDescription() || !sm.getDescription().equals(module.getDescription())) {
            module.setDescription(sm.getDescription());
            updated = true;
        }
        if (null == sm.getVendor() || !sm.getVendor().equals(module.getVendor())) {
            module.setVendor(sm.getVendor());
            updated = true;
        }

        return updated ? softwareModuleRepository.save(module) : module;
    }

    /**
     * Updates existing {@link SoftwareModuleType}. Update-able value is
     * {@link SoftwareModuleType#getDescription()} and
     * {@link SoftwareModuleType#getColour()}.
     *
     * @param sm
     *            to update
     * @return updated {@link Entity}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public SoftwareModuleType updateSoftwareModuleType(@NotNull final SoftwareModuleType sm) {
        checkNotNull(sm.getId());

        final SoftwareModuleType type = softwareModuleTypeRepository.findOne(sm.getId());

        boolean updated = false;
        if (sm.getDescription() != null && !sm.getDescription().equals(type.getDescription())) {
            type.setDescription(sm.getDescription());
            updated = true;
        }
        if (sm.getColour() != null && !sm.getColour().equals(type.getColour())) {
            type.setColour(sm.getColour());
            updated = true;
        }
        return updated ? softwareModuleTypeRepository.save(type) : type;
    }

    /**
     *
     * @param swModule
     *            SoftwareModule to create
     * @return SoftwareModule
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public SoftwareModule createSoftwareModule(@NotNull final SoftwareModule swModule) {
        if (null != swModule.getId()) {
            throw new EntityAlreadyExistsException();
        }
        return softwareModuleRepository.save(swModule);
    }

    /**
     * Create {@link SoftwareModule}s in the repository.
     *
     * @param swModules
     *            {@link SoftwareModule}s to create
     * @return SoftwareModule
     * @throws EntityAlreadyExistsException
     *             if a given entity already exists
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<SoftwareModule> createSoftwareModule(@NotNull final Iterable<SoftwareModule> swModules) {
        swModules.forEach(swModule -> {
            if (null != swModule.getId()) {
                throw new EntityAlreadyExistsException();
            }
        });

        return softwareModuleRepository.save(swModules);

    }

    /**
     * retrieves the {@link SoftwareModule}s by their {@link SoftwareModuleType}
     * .
     *
     * @param pageable
     *            page parameters
     * @param type
     *            to be filtered on
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Slice<SoftwareModule> findSoftwareModulesByType(@NotNull final Pageable pageable,
            @NotNull final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.equalType(type);
        specList.add(spec);

        spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return findSwModuleByCriteriaAPI(pageable, specList);
    }

    /**
     * Counts {@link SoftwareModule}s with given {@link SoftwareModuleType}.
     *
     * @param type
     *            to count
     * @return number of found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countSoftwareModulesByType(@NotNull final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.equalType(type);
        specList.add(spec);

        spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return countSwModuleByCriteriaAPI(specList);
    }

    /**
     * Finds {@link SoftwareModule} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link SoftwareModule}s or <code>null</code> if not
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public SoftwareModule findSoftwareModuleById(@NotNull final Long id) {
        return artifactManagement.findSoftwareModuleById(id);
    }

    /**
     * retrieves {@link SoftwareModule}s by their name AND version.
     *
     * @param name
     *            of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param type
     *            of the {@link SoftwareModule}
     * @return the found {@link SoftwareModule} or <code>null</code>
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModule findSoftwareModuleByNameAndVersion(@NotEmpty final String name,
            @NotEmpty final String version, @NotNull final SoftwareModuleType type) {

        return softwareModuleRepository.findOneByNameAndVersionAndType(name, version, type);
    }

    /**
     * Deletes the given {@link SoftwareModule} {@link Entity}.
     *
     * @param bsm
     *            is the {@link SoftwareModule} to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteSoftwareModule(@NotNull final SoftwareModule bsm) {

        deleteSoftwareModules(Sets.newHashSet(bsm.getId()));
    }

    private boolean isUnassigned(final SoftwareModule bsmMerged) {
        return distributionSetRepository.findByModules(bsmMerged).isEmpty();
    }

    private Slice<SoftwareModule> findSwModuleByCriteriaAPI(@NotNull final Pageable pageable,
            @NotEmpty final List<Specification<SoftwareModule>> specList) {
        return criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable,
                SoftwareModule.class);
    }

    private Long countSwModuleByCriteriaAPI(@NotEmpty final List<Specification<SoftwareModule>> specList) {
        return softwareModuleRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    private void deleteGridFsArtifacts(final SoftwareModule swModule) {
        for (final LocalArtifact localArtifact : swModule.getLocalArtifacts()) {
            artifactManagement.deleteGridFsArtifact(localArtifact);
        }
    }

    /**
     * Deletes {@link SoftwareModule}s which is any if the given ids.
     *
     * @param ids
     *            of the Software Moduels to be deleted
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteSoftwareModules(@NotNull final Iterable<Long> ids) {
        final List<SoftwareModule> swModulesToDelete = softwareModuleRepository.findByIdIn(ids);
        final Set<Long> assignedModuleIds = new HashSet<>();
        swModulesToDelete.forEach(swModule -> {

            // delete binary data of artifacts
            deleteGridFsArtifacts(swModule);

            if (isUnassigned(swModule)) {

                softwareModuleRepository.delete(swModule);

            } else {

                assignedModuleIds.add(swModule.getId());
            }
        });

        if (!assignedModuleIds.isEmpty()) {
            String currentUser = null;
            if (auditorProvider != null) {
                currentUser = auditorProvider.getCurrentAuditor();
            }
            softwareModuleRepository.deleteSoftwareModule(System.currentTimeMillis(), currentUser,
                    assignedModuleIds.toArray(new Long[0]));
        }
    }

    /**
     * Retrieves all software modules. Deleted ones are filtered.
     *
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Slice<SoftwareModule> findSoftwareModulesAll(@NotNull final Pageable pageable) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(SoftwareModule_.type);
            }
            return cb.conjunction();
        };

        specList.add(spec);

        return findSwModuleByCriteriaAPI(pageable, specList);
    }

    /**
     * Count all {@link SoftwareModule}s in the repository that are not marked
     * as deleted.
     *
     * @return number of {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countSoftwareModulesAll() {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        final Specification<SoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return countSwModuleByCriteriaAPI(specList);
    }

    /**
     * Retrieves software module including details (
     * {@link SoftwareModule#getArtifacts()}).
     *
     * @param id
     *            parameter
     * @param isDeleted
     *            parameter
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModule findSoftwareModuleWithDetails(@NotNull final Long id) {
        return artifactManagement.findSoftwareModuleWithDetails(id);
    }

    /**
     * Retrieves all {@link SoftwareModule}s with a given specification.
     *
     * @param spec
     *            the specification to filter the software modules
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModule> findSoftwareModulesByPredicate(@NotNull final Specification<SoftwareModule> spec,
            @NotNull final Pageable pageable) {
        return softwareModuleRepository.findAll(spec, pageable);
    }

    /**
     * Retrieves all {@link SoftwareModuleType}s with a given specification.
     *
     * @param spec
     *            the specification to filter the software modules types
     * @param pageable
     *            pagination parameter
     * @return the found {@link SoftwareModuleType}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModuleType> findSoftwareModuleTypesByPredicate(
            @NotNull final Specification<SoftwareModuleType> spec, @NotNull final Pageable pageable) {
        return softwareModuleTypeRepository.findAll(spec, pageable);
    }

    /**
     * Retrieves all software modules with a given list of ids
     * {@link SoftwareModule#getId()}.
     *
     * @param ids
     *            to search for
     * @return {@link List} of found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<SoftwareModule> findSoftwareModulesById(@NotEmpty final List<Long> ids) {
        return softwareModuleRepository.findByIdIn(ids);
    }

    /**
     * Filter {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * and {@link SoftwareModule#getType()} that are not marked as deleted.
     *
     * @param pageable
     *            page parameter
     * @param searchText
     *            to be filtered as "like" on {@link SoftwareModule#getName()}
     * @param type
     *            to be filtered as "like" on {@link SoftwareModule#getType()}
     * @return the page of found {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Slice<SoftwareModule> findSoftwareModuleByFilters(@NotNull final Pageable pageable, final String searchText,
            final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        if (!Strings.isNullOrEmpty(searchText)) {
            spec = SoftwareModuleSpecification.likeNameOrVersion(searchText);
            specList.add(spec);
        }

        if (null != type) {
            spec = SoftwareModuleSpecification.equalType(type);
            specList.add(spec);
        }

        spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(SoftwareModule_.type);
            }
            return cb.conjunction();
        };

        specList.add(spec);

        return findSwModuleByCriteriaAPI(pageable, specList);
    }

    /**
     * Filter {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * search text and {@link SoftwareModule#getType()} that are not marked as
     * deleted and sort them by means of given distribution set related modules
     * on top of the list.
     * 
     * After that the modules are sorted by {@link SoftwareModule#getName()} and
     * {@link SoftwareModule#getVersion()} in ascending order.
     *
     * @param pageable
     *            page parameter
     * @param orderByDistributionId
     *            the ID of distribution set to be ordered on top
     * @param searchText
     *            filtered as "like" on {@link SoftwareModule#getName()}
     * @param type
     *            filtered as "equal" on {@link SoftwareModule#getType()}
     * @return the page of found {@link SoftwareModule}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Slice<CustomSoftwareModule> findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
            @NotNull final Pageable pageable, @NotNull final Long orderByDistributionId, final String searchText,
            final SoftwareModuleType type) {

        final List<CustomSoftwareModule> resultList = new ArrayList<>();
        final int pageSize = pageable.getPageSize();
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // get the assigned software modules
        final CriteriaQuery<SoftwareModule> assignedQuery = cb.createQuery(SoftwareModule.class);
        final Root<SoftwareModule> assignedRoot = assignedQuery.from(SoftwareModule.class);
        assignedQuery.distinct(true);
        final ListJoin<SoftwareModule, DistributionSet> assignedDsJoin = assignedRoot.join(SoftwareModule_.assignedTo);
        // build the specifications and then to predicates necessary by the
        // given filters
        final Predicate[] specPredicate = specificationsToPredicate(buildSpecificationList(searchText, type),
                assignedRoot, assignedQuery, cb,
                cb.equal(assignedDsJoin.get(DistributionSet_.id), orderByDistributionId));
        // if we have some predicates then add it to the where clause of the
        // multi select
        assignedQuery.where(specPredicate);
        assignedQuery.orderBy(cb.asc(assignedRoot.get(SoftwareModule_.name)),
                cb.asc(assignedRoot.get(SoftwareModule_.version)));
        // don't page the assigned query on database, we need all assigned
        // software modules to filter
        // them out in the unassigned query
        final List<SoftwareModule> assignedSoftwareModules = entityManager.createQuery(assignedQuery).getResultList();
        // map result
        if (pageable.getOffset() < assignedSoftwareModules.size()) {
            assignedSoftwareModules
                    .subList(pageable.getOffset(), Math.min(assignedSoftwareModules.size(), pageable.getPageSize()))
                    .forEach(sw -> resultList.add(new CustomSoftwareModule(sw, true)));
        }

        if (assignedSoftwareModules.size() >= pageSize) {
            return new SliceImpl<>(resultList);
        }

        // get the unassigned software modules
        final CriteriaQuery<SoftwareModule> unassignedQuery = cb.createQuery(SoftwareModule.class);
        unassignedQuery.distinct(true);
        final Root<SoftwareModule> unassignedRoot = unassignedQuery.from(SoftwareModule.class);

        Predicate[] unassignedSpec;
        if (!assignedSoftwareModules.isEmpty()) {
            unassignedSpec = specificationsToPredicate(buildSpecificationList(searchText, type), unassignedRoot,
                    unassignedQuery, cb, cb.not(unassignedRoot.get(SoftwareModule_.id)
                            .in(assignedSoftwareModules.stream().map(sw -> sw.getId()).collect(Collectors.toList()))));
        } else {
            unassignedSpec = specificationsToPredicate(buildSpecificationList(searchText, type), unassignedRoot,
                    unassignedQuery, cb);
        }

        unassignedQuery.where(unassignedSpec);
        unassignedQuery.orderBy(cb.asc(unassignedRoot.get(SoftwareModule_.name)),
                cb.asc(unassignedRoot.get(SoftwareModule_.version)));
        final List<SoftwareModule> unassignedSoftwareModules = entityManager.createQuery(unassignedQuery)
                .setFirstResult(Math.max(0, pageable.getOffset() - assignedSoftwareModules.size()))
                .setMaxResults(pageSize).getResultList();
        // map result
        unassignedSoftwareModules.forEach(sw -> resultList.add(new CustomSoftwareModule(sw, false)));

        return new SliceImpl<>(resultList);
    }

    private List<Specification<SoftwareModule>> buildSpecificationList(final String searchText,
            final SoftwareModuleType type) {
        final List<Specification<SoftwareModule>> specList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(searchText)) {
            specList.add(SoftwareModuleSpecification.likeNameOrVersion(searchText));
        }
        if (type != null) {
            specList.add(SoftwareModuleSpecification.equalType(type));
        }
        specList.add(SoftwareModuleSpecification.isDeletedFalse());
        return specList;
    }

    /**
     * @param specifications
     */
    private Predicate[] specificationsToPredicate(final List<Specification<SoftwareModule>> specifications,
            final Root<SoftwareModule> root, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Predicate... additionalPredicates) {
        final List<Predicate> predicates = new ArrayList<>();
        specifications.forEach(spec -> predicates.add(spec.toPredicate(root, query, cb)));
        for (final Predicate predicate : additionalPredicates) {
            predicates.add(predicate);
        }
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    /**
     * Counts {@link SoftwareModule}s with given
     * {@link SoftwareModule#getName()} or {@link SoftwareModule#getVersion()}
     * and {@link SoftwareModule#getType()} that are not marked as deleted.
     *
     * @param searchText
     *            to search for in name and version
     * @param type
     *            to filter the result
     * @return number of found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countSoftwareModuleByFilters(final String searchText, final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        if (!Strings.isNullOrEmpty(searchText)) {
            spec = SoftwareModuleSpecification.likeNameOrVersion(searchText);
            specList.add(spec);
        }

        if (null != type) {
            spec = SoftwareModuleSpecification.equalType(type);
            specList.add(spec);
        }

        return countSwModuleByCriteriaAPI(specList);
    }

    /**
     * @param pageable
     *            parameter
     * @return all {@link SoftwareModuleType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModuleType> findSoftwareModuleTypesAll(@NotNull final Pageable pageable) {
        return softwareModuleTypeRepository.findByDeleted(pageable, false);
    }

    /**
     * @return number of {@link SoftwareModuleType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Long countSoftwareModuleTypesAll() {
        return softwareModuleTypeRepository.countByDeleted(false);
    }

    /**
     *
     * @param key
     *            to search for
     * @return {@link SoftwareModuleType} in the repository with given
     *         {@link SoftwareModuleType#getKey()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModuleType findSoftwareModuleTypeByKey(@NotNull final String key) {
        return softwareModuleTypeRepository.findByKey(key);
    }

    /**
     *
     * @param id
     *            to search for
     * @return {@link SoftwareModuleType} in the repository with given
     *         {@link SoftwareModuleType#getId()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModuleType findSoftwareModuleTypeById(@NotNull final Long id) {
        return softwareModuleTypeRepository.findOne(id);
    }

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getName()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModuleType findSoftwareModuleTypeByName(@NotNull final String name) {
        return softwareModuleTypeRepository.findByName(name);
    }

    /**
     * Creates new {@link SoftwareModuleType}.
     *
     * @param type
     *            to create
     * @return created {@link Entity}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public SoftwareModuleType createSoftwareModuleType(@NotNull final SoftwareModuleType type) {
        if (type.getId() != null) {
            throw new EntityAlreadyExistsException("Given type contains an Id!");
        }

        return softwareModuleTypeRepository.save(type);
    }

    /**
     * Creates multiple {@link SoftwareModuleType}s.
     *
     * @param types
     *            to create
     * @return created {@link Entity}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<SoftwareModuleType> createSoftwareModuleType(@NotNull final Collection<SoftwareModuleType> types) {
        return types.stream().map(this::createSoftwareModuleType).collect(Collectors.toList());
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
    public void deleteSoftwareModuleType(@NotNull final SoftwareModuleType type) {

        if (softwareModuleRepository.countByType(type) > 0
                || distributionSetTypeRepository.countByElementsSmType(type) > 0) {
            final SoftwareModuleType toDelete = entityManager.merge(type);
            toDelete.setDeleted(true);
            softwareModuleTypeRepository.save(toDelete);
        } else {
            softwareModuleTypeRepository.delete(type.getId());
        }
    }

    /**
     * @param pageable
     *            the page request to page the result set
     * @param set
     *            to search for
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModule> findSoftwareModuleByAssignedTo(@NotNull final Pageable pageable,
            @NotNull final DistributionSet set) {
        return softwareModuleRepository.findByAssignedTo(pageable, set);
    }

    /**
     * @param pageable
     *            the page request to page the result set
     * @param set
     *            to search for
     * @param type
     *            to filter
     * @return all {@link SoftwareModule}s that are assigned to given
     *         {@link DistributionSet} filtered by {@link SoftwareModuleType}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModule> findSoftwareModuleByAssignedToAndType(@NotNull final Pageable pageable,
            @NotNull final DistributionSet set, @NotNull final SoftwareModuleType type) {
        return softwareModuleRepository.findByAssignedToAndType(pageable, set, type);
    }

    /**
     * creates or updates a single software module meta data entry.
     *
     * @param metadata
     *            the meta data entry to create or update
     * @return the updated or created software module meta data entry
     * @throws EntityAlreadyExistsException
     *             in case the meta data entry already exists for the specific
     *             key
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public SoftwareModuleMetadata createSoftwareModuleMetadata(@NotNull final SoftwareModuleMetadata metadata) {
        if (softwareModuleMetadataRepository.exists(metadata.getId())) {
            throwMetadataKeyAlreadyExists(metadata.getId().getKey());
        }
        // merge base software module so optLockRevision gets updated and audit
        // log written because
        // modifying metadata is modifying the base software module itself for
        // auditing purposes.
        entityManager.merge(metadata.getSoftwareModule()).setLastModifiedAt(-1L);
        return softwareModuleMetadataRepository.save(metadata);
    }

    /**
     * creates a list of software module meta data entries.
     *
     * @param metadata
     *            the meta data entries to create or update
     * @return the updated or created software module meta data entries
     * @throws EntityAlreadyExistsException
     *             in case one of the meta data entry already exists for the
     *             specific key
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public List<SoftwareModuleMetadata> createSoftwareModuleMetadata(
            @NotEmpty final Collection<SoftwareModuleMetadata> metadata) {
        for (final SoftwareModuleMetadata softwareModuleMetadata : metadata) {
            checkAndThrowAlreadyExistsIfSoftwareModuleMetadataExists(softwareModuleMetadata.getId());
        }
        metadata.forEach(m -> entityManager.merge(m.getSoftwareModule()).setLastModifiedAt(-1L));
        return softwareModuleMetadataRepository.save(metadata);
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
    public SoftwareModuleMetadata updateSoftwareModuleMetadata(@NotNull final SoftwareModuleMetadata metadata) {
        // check if exists otherwise throw entity not found exception
        findSoftwareModuleMetadata(metadata.getId());
        // touch it to update the lock revision because we are modifying the
        // software module
        // indirectly
        entityManager.merge(metadata.getSoftwareModule()).setLastModifiedAt(-1L);
        return softwareModuleMetadataRepository.save(metadata);
    }

    /**
     * deletes a software module meta data entry.
     *
     * @param id
     *            the ID of the software module meta data to delete
     */
    @Transactional
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public void deleteSoftwareModuleMetadata(@NotNull final SwMetadataCompositeKey id) {
        softwareModuleMetadataRepository.delete(id);
    }

    /**
     * finds all meta data by the given software module id.
     *
     * @param swId
     *            the software module id to retrieve the meta data from
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given software
     *         module id
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(@NotNull final Long swId,
            @NotNull final Pageable pageable) {
        return softwareModuleMetadataRepository.findBySoftwareModuleId(swId, pageable);
    }

    /**
     * finds all meta data by the given software module id.
     *
     * @param softwareModuleId
     *            the software module id to retrieve the meta data from
     * @param spec
     *            the specification to filter the result
     * @param pageable
     *            the page request to page the result
     * @return a paged result of all meta data entries for a given software
     *         module id
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(final Long softwareModuleId,
            @NotNull final Specification<SoftwareModuleMetadata> spec, @NotNull final Pageable pageable) {
        return softwareModuleMetadataRepository
                .findAll(
                        (Specification<SoftwareModuleMetadata>) (root, query,
                                cb) -> cb.and(
                                        cb.equal(root.get(SoftwareModuleMetadata_.softwareModule)
                                                .get(SoftwareModule_.id), softwareModuleId),
                                        spec.toPredicate(root, query, cb)),
                        pageable);
    }

    /**
     * finds a single software module meta data by its id.
     *
     * @param id
     *            the id of the software module meta data containing the meta
     *            data key and the ID of the software module
     * @return the found SoftwareModuleMetadata or {@code null} if not exits
     * @throws EntityNotFoundException
     *             in case the meta data does not exists for the given key
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public SoftwareModuleMetadata findSoftwareModuleMetadata(@NotNull final SwMetadataCompositeKey id) {
        final SoftwareModuleMetadata findOne = softwareModuleMetadataRepository.findOne(id);
        if (findOne == null) {
            throw new EntityNotFoundException("Metadata with key '" + id.getKey() + "' does not exist");
        }
        return findOne;
    }

    private void checkAndThrowAlreadyExistsIfSoftwareModuleMetadataExists(final SwMetadataCompositeKey metadataId) {
        if (softwareModuleMetadataRepository.exists(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    private void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

}
