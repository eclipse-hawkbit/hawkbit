/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * JPA implementation of {@link SoftwareManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class JpaSoftwareManagement implements SoftwareManagement {

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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public SoftwareModule updateSoftwareModule(final SoftwareModule sm) {
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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public SoftwareModuleType updateSoftwareModuleType(final SoftwareModuleType sm) {
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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public SoftwareModule createSoftwareModule(final SoftwareModule swModule) {
        if (null != swModule.getId()) {
            throw new EntityAlreadyExistsException();
        }
        return softwareModuleRepository.save(swModule);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<SoftwareModule> createSoftwareModule(final Collection<SoftwareModule> swModules) {
        swModules.forEach(swModule -> {
            if (null != swModule.getId()) {
                throw new EntityAlreadyExistsException();
            }
        });

        return softwareModuleRepository.save(swModules);

    }

    @Override
    public Slice<SoftwareModule> findSoftwareModulesByType(final Pageable pageable, final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.equalType(type);
        specList.add(spec);

        spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return findSwModuleByCriteriaAPI(pageable, specList);
    }

    @Override
    public Long countSoftwareModulesByType(final SoftwareModuleType type) {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        Specification<SoftwareModule> spec = SoftwareModuleSpecification.equalType(type);
        specList.add(spec);

        spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return countSwModuleByCriteriaAPI(specList);
    }

    @Override
    public SoftwareModule findSoftwareModuleById(final Long id) {
        return artifactManagement.findSoftwareModuleById(id);
    }

    @Override
    public SoftwareModule findSoftwareModuleByNameAndVersion(final String name, final String version,
            final SoftwareModuleType type) {

        return softwareModuleRepository.findOneByNameAndVersionAndType(name, version, type);
    }

    private boolean isUnassigned(final SoftwareModule bsmMerged) {
        return distributionSetRepository.findByModules(bsmMerged).isEmpty();
    }

    private Slice<SoftwareModule> findSwModuleByCriteriaAPI(final Pageable pageable,
            final List<Specification<SoftwareModule>> specList) {
        return criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable,
                SoftwareModule.class);
    }

    private Long countSwModuleByCriteriaAPI(final List<Specification<SoftwareModule>> specList) {
        return softwareModuleRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    private void deleteGridFsArtifacts(final SoftwareModule swModule) {
        for (final LocalArtifact localArtifact : swModule.getLocalArtifacts()) {
            artifactManagement.deleteLocalArtifact(localArtifact);
        }
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteSoftwareModules(final Collection<Long> ids) {
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

    @Override
    public Slice<SoftwareModule> findSoftwareModulesAll(final Pageable pageable) {

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

    @Override
    public Long countSoftwareModulesAll() {

        final List<Specification<SoftwareModule>> specList = new ArrayList<>();

        final Specification<SoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        return countSwModuleByCriteriaAPI(specList);
    }

    @Override
    public SoftwareModule findSoftwareModuleWithDetails(final Long id) {
        return artifactManagement.findSoftwareModuleWithDetails(id);
    }

    @Override
    public Page<SoftwareModule> findSoftwareModulesByPredicate(final Specification<SoftwareModule> spec,
            final Pageable pageable) {
        return softwareModuleRepository.findAll(spec, pageable);
    }

    @Override
    public Page<SoftwareModuleType> findSoftwareModuleTypesByPredicate(final Specification<SoftwareModuleType> spec,
            final Pageable pageable) {
        return softwareModuleTypeRepository.findAll(spec, pageable);
    }

    @Override
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<SoftwareModule> findSoftwareModulesById(final Collection<Long> ids) {
        return softwareModuleRepository.findByIdIn(ids);
    }

    @Override
    public Slice<SoftwareModule> findSoftwareModuleByFilters(final Pageable pageable, final String searchText,
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

    @Override
    public Slice<CustomSoftwareModule> findSoftwareModuleOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
            final Pageable pageable, final Long orderByDistributionId, final String searchText,
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

    private static List<Specification<SoftwareModule>> buildSpecificationList(final String searchText,
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

    @Override
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

    @Override
    public Page<SoftwareModuleType> findSoftwareModuleTypesAll(final Pageable pageable) {
        return softwareModuleTypeRepository.findByDeleted(pageable, false);
    }

    @Override
    public Long countSoftwareModuleTypesAll() {
        return softwareModuleTypeRepository.countByDeleted(false);
    }

    @Override
    public SoftwareModuleType findSoftwareModuleTypeByKey(final String key) {
        return softwareModuleTypeRepository.findByKey(key);
    }

    @Override
    public SoftwareModuleType findSoftwareModuleTypeById(final Long id) {
        return softwareModuleTypeRepository.findOne(id);
    }

    @Override
    public SoftwareModuleType findSoftwareModuleTypeByName(final String name) {
        return softwareModuleTypeRepository.findByName(name);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public SoftwareModuleType createSoftwareModuleType(final SoftwareModuleType type) {
        if (type.getId() != null) {
            throw new EntityAlreadyExistsException("Given type contains an Id!");
        }

        return softwareModuleTypeRepository.save(type);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteSoftwareModuleType(final SoftwareModuleType type) {

        if (softwareModuleRepository.countByType(type) > 0
                || distributionSetTypeRepository.countByElementsSmType(type) > 0) {
            final SoftwareModuleType toDelete = entityManager.merge(type);
            toDelete.setDeleted(true);
            softwareModuleTypeRepository.save(toDelete);
        } else {
            softwareModuleTypeRepository.delete(type.getId());
        }
    }

    @Override
    public Page<SoftwareModule> findSoftwareModuleByAssignedTo(final Pageable pageable, final DistributionSet set) {
        return softwareModuleRepository.findByAssignedTo(pageable, set);
    }

    @Override
    public Page<SoftwareModule> findSoftwareModuleByAssignedToAndType(final Pageable pageable,
            final DistributionSet set, final SoftwareModuleType type) {
        return softwareModuleRepository.findByAssignedToAndType(pageable, set, type);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public SoftwareModuleMetadata createSoftwareModuleMetadata(final SoftwareModuleMetadata metadata) {
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

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public List<SoftwareModuleMetadata> createSoftwareModuleMetadata(
            final Collection<SoftwareModuleMetadata> metadata) {
        for (final SoftwareModuleMetadata softwareModuleMetadata : metadata) {
            checkAndThrowAlreadyExistsIfSoftwareModuleMetadataExists(softwareModuleMetadata.getId());
        }
        metadata.forEach(m -> entityManager.merge(m.getSoftwareModule()).setLastModifiedAt(-1L));
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public SoftwareModuleMetadata updateSoftwareModuleMetadata(final SoftwareModuleMetadata metadata) {
        // check if exists otherwise throw entity not found exception
        findSoftwareModuleMetadata(metadata.getId());
        // touch it to update the lock revision because we are modifying the
        // software module
        // indirectly
        entityManager.merge(metadata.getSoftwareModule()).setLastModifiedAt(-1L);
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void deleteSoftwareModuleMetadata(final SwMetadataCompositeKey id) {
        softwareModuleMetadataRepository.delete(id);
    }

    @Override
    public Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(final Long swId,
            final Pageable pageable) {
        return softwareModuleMetadataRepository.findBySoftwareModuleId(swId, pageable);
    }

    @Override
    public Page<SoftwareModuleMetadata> findSoftwareModuleMetadataBySoftwareModuleId(final Long softwareModuleId,
            final Specification<SoftwareModuleMetadata> spec, final Pageable pageable) {
        return softwareModuleMetadataRepository
                .findAll(
                        (Specification<SoftwareModuleMetadata>) (root, query,
                                cb) -> cb.and(
                                        cb.equal(root.get(SoftwareModuleMetadata_.softwareModule)
                                                .get(SoftwareModule_.id), softwareModuleId),
                                        spec.toPredicate(root, query, cb)),
                        pageable);
    }

    @Override
    public SoftwareModuleMetadata findSoftwareModuleMetadata(final SwMetadataCompositeKey id) {
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

    private static void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteSoftwareModule(final SoftwareModule bsm) {
        deleteSoftwareModules(Sets.newHashSet(bsm.getId()));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<SoftwareModuleType> createSoftwareModuleType(final Collection<SoftwareModuleType> types) {

        return types.stream().map(this::createSoftwareModuleType).collect(Collectors.toList());
    }

}
