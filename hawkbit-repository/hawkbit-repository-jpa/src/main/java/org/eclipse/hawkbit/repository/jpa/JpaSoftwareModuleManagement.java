/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.jpa.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;

/**
 * JPA implementation of {@link SoftwareModuleManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleManagement implements SoftwareModuleManagement {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

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

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModule update(final SoftwareModuleUpdate u) {
        final GenericSoftwareModuleUpdate update = (GenericSoftwareModuleUpdate) u;

        final JpaSoftwareModule module = softwareModuleRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, update.getId()));

        update.getDescription().ifPresent(module::setDescription);
        update.getVendor().ifPresent(module::setVendor);

        return softwareModuleRepository.save(module);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModule create(final SoftwareModuleCreate c) {
        final JpaSoftwareModuleCreate create = (JpaSoftwareModuleCreate) c;

        return softwareModuleRepository.save(create.build());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModule> create(final Collection<SoftwareModuleCreate> swModules) {
        return swModules.stream().map(this::create).collect(Collectors.toList());
    }

    @Override
    public Slice<SoftwareModule> findByType(final Pageable pageable, final long typeId) {
        throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

        final List<Specification<JpaSoftwareModule>> specList = Lists.newArrayListWithExpectedSize(2);

        specList.add(SoftwareModuleSpecification.equalType(typeId));
        specList.add(SoftwareModuleSpecification.isDeletedFalse());

        return convertSmPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    private void throwExceptionIfSoftwareModuleTypeDoesNotExist(final Long typeId) {
        if (!softwareModuleTypeRepository.exists(typeId)) {
            throw new EntityNotFoundException(SoftwareModuleType.class, typeId);
        }
    }

    private static Slice<SoftwareModule> convertSmPage(final Slice<JpaSoftwareModule> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, 0);
    }

    private static Page<SoftwareModule> convertSmPage(final Page<JpaSoftwareModule> findAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    private static Page<SoftwareModuleMetadata> convertSmMdPage(final Page<JpaSoftwareModuleMetadata> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Optional<SoftwareModule> get(final long id) {
        return Optional.ofNullable(softwareModuleRepository.findOne(id));
    }

    @Override
    public Optional<SoftwareModule> getByNameAndVersionAndType(final String name, final String version,
            final long typeId) {

        throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

        return softwareModuleRepository.findOneByNameAndVersionAndTypeId(name, version, typeId);
    }

    private boolean isUnassigned(final Long moduleId) {
        return distributionSetRepository.countByModulesId(moduleId) <= 0;
    }

    private Slice<JpaSoftwareModule> findByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaSoftwareModule>> specList) {
        return criteriaNoCountDao.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable,
                JpaSoftwareModule.class);
    }

    private Long countSwModuleByCriteriaAPI(final List<Specification<JpaSoftwareModule>> specList) {
        return softwareModuleRepository.count(SpecificationsBuilder.combineWithAnd(specList));
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {
        for (final Artifact localArtifact : swModule.getArtifacts()) {
            artifactManagement.clearArtifactBinary(localArtifact.getSha1Hash(), swModule.getId());
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        final List<JpaSoftwareModule> swModulesToDelete = softwareModuleRepository.findByIdIn(ids);

        if (swModulesToDelete.size() < ids.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, ids,
                    swModulesToDelete.stream().map(SoftwareModule::getId).collect(Collectors.toList()));
        }

        final Set<Long> assignedModuleIds = new HashSet<>();
        swModulesToDelete.forEach(swModule -> {

            // delete binary data of artifacts
            deleteGridFsArtifacts(swModule);

            if (isUnassigned(swModule.getId())) {
                softwareModuleRepository.delete(swModule.getId());
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
    public Slice<SoftwareModule> findAll(final Pageable pageable) {

        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(2);

        Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(JpaSoftwareModule_.type);
            }
            return cb.conjunction();
        };

        specList.add(spec);

        return convertSmPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public long count() {
        final Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();

        return countSwModuleByCriteriaAPI(Arrays.asList(spec));
    }

    @Override
    public Page<SoftwareModule> findByRsql(final Pageable pageable, final String rsqlParam) {
        final Specification<JpaSoftwareModule> spec = RSQLUtility.parse(rsqlParam, SoftwareModuleFields.class,
                virtualPropertyReplacer);

        return convertSmPage(softwareModuleRepository.findAll(spec, pageable), pageable);
    }

    @Override
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<SoftwareModule> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleRepository.findByIdIn(ids));
    }

    @Override
    public Slice<SoftwareModule> findByTextAndType(final Pageable pageable, final String searchText,
            final Long typeId) {

        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(4);

        Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        if (!StringUtils.isEmpty(searchText)) {
            spec = SoftwareModuleSpecification.likeNameOrVersion(searchText);
            specList.add(spec);
        }

        if (null != typeId) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

            spec = SoftwareModuleSpecification.equalType(typeId);
            specList.add(spec);
        }

        spec = (root, query, cb) -> {
            if (!query.getResultType().isAssignableFrom(Long.class)) {
                root.fetch(JpaSoftwareModule_.type);
            }
            return cb.conjunction();
        };

        specList.add(spec);

        return convertSmPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    @Override
    public Slice<AssignedSoftwareModule> findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
            final Pageable pageable, final long orderByDistributionId, final String searchText, final Long typeId) {

        final List<AssignedSoftwareModule> resultList = new ArrayList<>();
        final int pageSize = pageable.getPageSize();
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // get the assigned software modules
        final CriteriaQuery<JpaSoftwareModule> assignedQuery = cb.createQuery(JpaSoftwareModule.class);
        final Root<JpaSoftwareModule> assignedRoot = assignedQuery.from(JpaSoftwareModule.class);
        assignedQuery.distinct(true);
        final ListJoin<JpaSoftwareModule, JpaDistributionSet> assignedDsJoin = assignedRoot
                .join(JpaSoftwareModule_.assignedTo);
        // build the specifications and then to predicates necessary by the
        // given filters
        final Predicate[] specPredicate = specificationsToPredicate(buildSpecificationList(searchText, typeId),
                assignedRoot, assignedQuery, cb,
                cb.equal(assignedDsJoin.get(JpaDistributionSet_.id), orderByDistributionId));
        // if we have some predicates then add it to the where clause of the
        // multi select
        assignedQuery.where(specPredicate);
        assignedQuery.orderBy(cb.asc(assignedRoot.get(JpaSoftwareModule_.name)),
                cb.asc(assignedRoot.get(JpaSoftwareModule_.version)));
        // don't page the assigned query on database, we need all assigned
        // software modules to filter
        // them out in the unassigned query
        final List<JpaSoftwareModule> assignedSoftwareModules = entityManager.createQuery(assignedQuery)
                .getResultList();
        // map result
        if (pageable.getOffset() < assignedSoftwareModules.size()) {
            assignedSoftwareModules
                    .subList(pageable.getOffset(), Math.min(assignedSoftwareModules.size(), pageable.getPageSize()))
                    .forEach(sw -> resultList.add(new AssignedSoftwareModule(sw, true)));
        }

        if (assignedSoftwareModules.size() >= pageSize) {
            return new SliceImpl<>(resultList);
        }

        // get the unassigned software modules
        final CriteriaQuery<JpaSoftwareModule> unassignedQuery = cb.createQuery(JpaSoftwareModule.class);
        unassignedQuery.distinct(true);
        final Root<JpaSoftwareModule> unassignedRoot = unassignedQuery.from(JpaSoftwareModule.class);

        Predicate[] unassignedSpec;
        if (!assignedSoftwareModules.isEmpty()) {
            unassignedSpec = specificationsToPredicate(buildSpecificationList(searchText, typeId), unassignedRoot,
                    unassignedQuery, cb, cb.not(unassignedRoot.get(JpaSoftwareModule_.id).in(
                            assignedSoftwareModules.stream().map(SoftwareModule::getId).collect(Collectors.toList()))));
        } else {
            unassignedSpec = specificationsToPredicate(buildSpecificationList(searchText, typeId), unassignedRoot,
                    unassignedQuery, cb);
        }

        unassignedQuery.where(unassignedSpec);
        unassignedQuery.orderBy(cb.asc(unassignedRoot.get(JpaSoftwareModule_.name)),
                cb.asc(unassignedRoot.get(JpaSoftwareModule_.version)));
        final List<JpaSoftwareModule> unassignedSoftwareModules = entityManager.createQuery(unassignedQuery)
                .setFirstResult(Math.max(0, pageable.getOffset() - assignedSoftwareModules.size()))
                .setMaxResults(pageSize).getResultList();
        // map result
        unassignedSoftwareModules.forEach(sw -> resultList.add(new AssignedSoftwareModule(sw, false)));

        return new SliceImpl<>(resultList);
    }

    private List<Specification<JpaSoftwareModule>> buildSpecificationList(final String searchText, final Long typeId) {
        final List<Specification<JpaSoftwareModule>> specList = Lists.newArrayListWithExpectedSize(3);
        if (!StringUtils.isEmpty(searchText)) {
            specList.add(SoftwareModuleSpecification.likeNameOrVersion(searchText));
        }
        if (typeId != null) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

            specList.add(SoftwareModuleSpecification.equalType(typeId));
        }
        specList.add(SoftwareModuleSpecification.isDeletedFalse());
        return specList;
    }

    private Predicate[] specificationsToPredicate(final List<Specification<JpaSoftwareModule>> specifications,
            final Root<JpaSoftwareModule> root, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Predicate... additionalPredicates) {

        return Stream.concat(specifications.stream().map(spec -> spec.toPredicate(root, query, cb)),
                Arrays.stream(additionalPredicates)).toArray(Predicate[]::new);
    }

    @Override
    public long countByTextAndType(final String searchText, final Long typeId) {

        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(3);

        Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.isDeletedFalse();
        specList.add(spec);

        if (!StringUtils.isEmpty(searchText)) {
            spec = SoftwareModuleSpecification.likeNameOrVersion(searchText);
            specList.add(spec);
        }

        if (null != typeId) {
            throwExceptionIfSoftwareModuleTypeDoesNotExist(typeId);

            spec = SoftwareModuleSpecification.equalType(typeId);
            specList.add(spec);
        }

        return countSwModuleByCriteriaAPI(specList);
    }

    @Override
    public Page<SoftwareModule> findByAssignedTo(final Pageable pageable, final long setId) {
        if (!distributionSetRepository.exists(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return softwareModuleRepository.findByAssignedToId(pageable, setId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata createMetaData(final SoftwareModuleMetadataCreate c) {
        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;

        checkAndThrowAlreadyIfSoftwareModuleMetadataExists(create.getSoftwareModuleId(), create);
        touch(create.getSoftwareModuleId());

        return softwareModuleMetadataRepository.save(create.build());
    }

    private void checkAndThrowAlreadyIfSoftwareModuleMetadataExists(final Long moduleId,
            final JpaSoftwareModuleMetadataCreate md) {
        if (softwareModuleMetadataRepository.exists(new SwMetadataCompositeKey(moduleId, md.getKey()))) {
            throwMetadataKeyAlreadyExists(md.getKey());
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleMetadata> createMetaData(final Collection<SoftwareModuleMetadataCreate> create) {

        return create.stream().map(this::createMetaData).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetaData(final SoftwareModuleMetadataUpdate u) {
        final GenericSoftwareModuleMetadataUpdate update = (GenericSoftwareModuleMetadataUpdate) u;

        // check if exists otherwise throw entity not found exception
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(
                update.getSoftwareModuleId(), update.getKey())
                        .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class,
                                update.getSoftwareModuleId(), update.getKey()));

        update.getValue().ifPresent(metadata::setValue);
        update.isTargetVisible().ifPresent(metadata::setTargetVisible);

        touch(metadata.getSoftwareModule());
        return softwareModuleMetadataRepository.save(metadata);
    }

    /**
     * Method to get the latest module based on ID after the metadata changes
     * for that module.
     *
     * @param latestModule
     *            module to touch
     */
    private JpaSoftwareModule touch(final SoftwareModule latestModule) {
        // merge base distribution set so optLockRevision gets updated and audit
        // log written because
        // modifying metadata is modifying the base distribution set itself for
        // auditing purposes.
        final JpaSoftwareModule result = entityManager.merge((JpaSoftwareModule) latestModule);
        result.setLastModifiedAt(0L);

        return result;
    }

    /**
     * Method to get the latest module based on ID after the metadata changes
     * for that module.
     *
     * @param moduleId
     *            of the module to touch
     */
    private JpaSoftwareModule touch(final Long moduleId) {
        return touch(get(moduleId).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId)));
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long moduleId, final String key) {
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(moduleId,
                key).orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class, moduleId, key));

        touch(metadata.getSoftwareModule());
        softwareModuleMetadataRepository.delete(metadata.getId());
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long swId) {
        if (!softwareModuleRepository.exists(swId)) {
            throw new EntityNotFoundException(SoftwareModule.class, swId);
        }
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataByRsql(final Pageable pageable, final long softwareModuleId,
            final String rsqlParam) {

        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        final Specification<JpaSoftwareModuleMetadata> spec = RSQLUtility.parse(rsqlParam,
                SoftwareModuleMetadataFields.class, virtualPropertyReplacer);
        return convertSmMdPage(
                softwareModuleMetadataRepository
                        .findAll(
                                (root, query, cb) -> cb.and(
                                        cb.equal(root.get(JpaSoftwareModuleMetadata_.softwareModule)
                                                .get(JpaSoftwareModule_.id), softwareModuleId),
                                        spec.toPredicate(root, query, cb)),
                                pageable),
                pageable);
    }

    private static Page<SoftwareModuleMetadata> convertMdPage(final Page<JpaSoftwareModuleMetadata> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleId(final Pageable pageable, final long swId) {
        throwExceptionIfSoftwareModuleDoesNotExist(swId);

        return convertMdPage(softwareModuleMetadataRepository.findAll(
                (Specification<JpaSoftwareModuleMetadata>) (root, query, cb) -> cb
                        .equal(root.get(JpaSoftwareModuleMetadata_.softwareModule).get(JpaSoftwareModule_.id), swId),
                pageable), pageable);
    }

    @Override
    public Optional<SoftwareModuleMetadata> getMetaDataBySoftwareModuleId(final long moduleId, final String key) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return Optional.ofNullable(softwareModuleMetadataRepository.findOne(new SwMetadataCompositeKey(moduleId, key)));
    }

    private static void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long moduleId) {
        delete(Arrays.asList(moduleId));
    }

    @Override
    public boolean exists(final long id) {
        return softwareModuleRepository.exists(id);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(final Pageable pageable,
            final long moduleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return convertMdPage(softwareModuleMetadataRepository.findBySoftwareModuleIdAndTargetVisible(
                new PageRequest(0, RepositoryConstants.MAX_META_DATA_COUNT), moduleId, true), pageable);
    }

}
