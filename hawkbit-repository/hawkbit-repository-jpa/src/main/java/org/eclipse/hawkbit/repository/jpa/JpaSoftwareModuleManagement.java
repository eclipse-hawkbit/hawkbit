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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.ArtifactEncryption;
import org.eclipse.hawkbit.repository.ArtifactEncryptionSecretsStore;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
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
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
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

    private final EntityManager entityManager;

    private final DistributionSetRepository distributionSetRepository;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;

    private final AuditorAware<String> auditorProvider;

    private final ArtifactManagement artifactManagement;

    private final QuotaManagement quotaManagement;

    private final VirtualPropertyReplacer virtualPropertyReplacer;

    private final Database database;

    private final ArtifactEncryption artifactEncryption;

    private final ArtifactEncryptionSecretsStore artifactEncryptionSecretsStore;

    public JpaSoftwareModuleManagement(final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final SoftwareModuleRepository softwareModuleRepository,
            final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository, final AuditorAware<String> auditorProvider,
            final ArtifactManagement artifactManagement, final QuotaManagement quotaManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database,
            final ArtifactEncryption artifactEncryption,
            final ArtifactEncryptionSecretsStore artifactEncryptionSecretsStore) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.softwareModuleMetadataRepository = softwareModuleMetadataRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.auditorProvider = auditorProvider;
        this.artifactManagement = artifactManagement;
        this.quotaManagement = quotaManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.artifactEncryption = artifactEncryption;
        this.artifactEncryptionSecretsStore = artifactEncryptionSecretsStore;
    }

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

        final JpaSoftwareModule sm = softwareModuleRepository.save(create.build());
        // flush sm creation in order to get an Id
        entityManager.flush();
        generateEncryptionSecretsIfRequested(create.isEncrypted().orElse(false), sm.getId());

        return sm;
    }

    private void generateEncryptionSecretsIfRequested(final boolean encryptionRequested, final long smId) {
        if (artifactEncryption != null && artifactEncryptionSecretsStore != null && encryptionRequested) {
            final Map<String, String> secrets = artifactEncryption.generateSecrets();
            secrets.forEach((key, value) -> artifactEncryptionSecretsStore.addSecret(smId, key, value));
            // we want to clear secrets from memory as soon as possible
            secrets.clear();
        }
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
        if (!softwareModuleTypeRepository.existsById(typeId)) {
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
        return softwareModuleRepository.findById(id).map(sm -> (SoftwareModule) sm);
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
        return softwareModuleRepository.findAllWithoutCount(SpecificationsBuilder.combineWithAnd(specList), pageable);
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
                softwareModuleRepository.deleteById(swModule.getId());
            } else {
                assignedModuleIds.add(swModule.getId());
            }
        });

        if (!assignedModuleIds.isEmpty()) {
            String currentUser = null;
            if (auditorProvider != null) {
                currentUser = auditorProvider.getCurrentAuditor().orElse(null);
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
        final Specification<JpaSoftwareModule> spec = RSQLUtility.buildRsqlSpecification(rsqlParam,
                SoftwareModuleFields.class, virtualPropertyReplacer, database);

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
            final Pageable pageable, final long dsId, final String searchText, final Long smTypeId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Tuple> query = cb.createTupleQuery();
        final Root<JpaSoftwareModule> smRoot = query.from(JpaSoftwareModule.class);

        final ListJoin<JpaSoftwareModule, JpaDistributionSet> assignedDsList = smRoot
                .join(JpaSoftwareModule_.assignedTo, JoinType.LEFT);

        final Expression<Integer> assignedCaseMax = cb.max(
                cb.<Long, Integer> selectCase(assignedDsList.get(JpaDistributionSet_.id)).when(dsId, 1).otherwise(0));

        query.multiselect(smRoot.alias("sm"), assignedCaseMax.alias("assigned"));

        final Predicate[] specPredicate = specificationsToPredicate(buildSpecificationList(searchText, smTypeId),
                smRoot, query, cb);

        if (specPredicate.length > 0) {
            query.where(specPredicate);
        }

        query.groupBy(smRoot);

        query.orderBy(cb.desc(assignedCaseMax), cb.asc(smRoot.get(JpaSoftwareModule_.name)),
                cb.asc(smRoot.get(JpaSoftwareModule_.version)));

        final int pageSize = pageable.getPageSize();
        final List<Tuple> smWithAssignedFlagList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageSize).getResultList();
        final boolean hasNext = smWithAssignedFlagList.size() > pageSize;

        final List<AssignedSoftwareModule> resultList = new ArrayList<>();

        smWithAssignedFlagList.forEach(smWithAssignedFlag -> resultList
                .add(new AssignedSoftwareModule(smWithAssignedFlag.get("sm", JpaSoftwareModule.class),
                        smWithAssignedFlag.get("assigned", Number.class).longValue() == 1)));

        return new SliceImpl<>(Collections.unmodifiableList(resultList), pageable, hasNext);
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
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return softwareModuleRepository.findByAssignedToId(pageable, setId);
    }

    @Override
    public long countByAssignedTo(final long setId) {
        if (!distributionSetRepository.existsById(setId)) {
            throw new EntityNotFoundException(DistributionSet.class, setId);
        }

        return softwareModuleRepository.countByAssignedToId(setId);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata createMetaData(final SoftwareModuleMetadataCreate c) {

        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;
        final Long moduleId = create.getSoftwareModuleId();
        assertSoftwareModuleExists(moduleId);
        assertMetaDataQuota(moduleId, 1);

        return saveMetadata(create);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleMetadata> createMetaData(final Collection<SoftwareModuleMetadataCreate> create) {

        if (!create.isEmpty()) {

            // check if all meta data entries refer to the same software module
            final Long moduleId = ((JpaSoftwareModuleMetadataCreate) create.iterator().next()).getSoftwareModuleId();
            if (createJpaMetadataCreateStream(create).allMatch(c -> moduleId.equals(c.getSoftwareModuleId()))) {

                assertSoftwareModuleExists(moduleId);
                assertMetaDataQuota(moduleId, create.size());

                return createJpaMetadataCreateStream(create).map(this::saveMetadata).collect(Collectors.toList());

            } else {

                // group by software module id to minimize database access
                final Map<Long, List<JpaSoftwareModuleMetadataCreate>> groups = createJpaMetadataCreateStream(create)
                        .collect(Collectors.groupingBy(JpaSoftwareModuleMetadataCreate::getSoftwareModuleId));
                return groups.entrySet().stream().flatMap(e -> {

                    final Long id = e.getKey();
                    final List<JpaSoftwareModuleMetadataCreate> group = e.getValue();

                    assertSoftwareModuleExists(id);
                    assertMetaDataQuota(id, group.size());

                    return group.stream().map(this::saveMetadata);
                }).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private static Stream<JpaSoftwareModuleMetadataCreate> createJpaMetadataCreateStream(
            final Collection<SoftwareModuleMetadataCreate> create) {
        return create.stream().map(c -> (JpaSoftwareModuleMetadataCreate) c);
    }

    private SoftwareModuleMetadata saveMetadata(final JpaSoftwareModuleMetadataCreate create) {
        assertSoftwareModuleMetadataDoesNotExist(create.getSoftwareModuleId(), create);
        return softwareModuleMetadataRepository.save(create.build());
    }

    private void assertSoftwareModuleMetadataDoesNotExist(final Long moduleId,
            final JpaSoftwareModuleMetadataCreate md) {
        if (softwareModuleMetadataRepository.existsById(new SwMetadataCompositeKey(moduleId, md.getKey()))) {
            throwMetadataKeyAlreadyExists(md.getKey());
        }
    }

    private void assertSoftwareModuleExists(final Long moduleId) {
        touch(moduleId);
    }

    /**
     * Asserts the meta data quota for the software module with the given ID.
     * 
     * @param moduleId
     *            The software module ID.
     * @param requested
     *            Number of meta data entries to be created.
     */
    private void assertMetaDataQuota(final Long moduleId, final int requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        QuotaHelper.assertAssignmentQuota(moduleId, requested, maxMetaData, SoftwareModuleMetadata.class,
                SoftwareModule.class, softwareModuleMetadataRepository::countBySoftwareModuleId);
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
        // log written because modifying metadata is modifying the base
        // distribution set itself for auditing purposes.
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
        softwareModuleMetadataRepository.deleteById(metadata.getId());
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long swId) {
        if (!softwareModuleRepository.existsById(swId)) {
            throw new EntityNotFoundException(SoftwareModule.class, swId);
        }
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataByRsql(final Pageable pageable, final long softwareModuleId,
            final String rsqlParam) {

        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        final Specification<JpaSoftwareModuleMetadata> spec = RSQLUtility.buildRsqlSpecification(rsqlParam,
                SoftwareModuleMetadataFields.class, virtualPropertyReplacer, database);
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

        return softwareModuleMetadataRepository.findById(new SwMetadataCompositeKey(moduleId, key))
                .map(smmd -> (SoftwareModuleMetadata) smmd);
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
        return softwareModuleRepository.existsById(id);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(final Pageable pageable,
            final long moduleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        return convertMdPage(softwareModuleMetadataRepository.findBySoftwareModuleIdAndTargetVisible(
                PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT), moduleId, true), pageable);
    }

}
