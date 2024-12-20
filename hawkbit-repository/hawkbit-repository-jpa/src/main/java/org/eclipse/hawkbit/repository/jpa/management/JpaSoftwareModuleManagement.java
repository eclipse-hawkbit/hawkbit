/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

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

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
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
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleCreate;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule_;
import org.eclipse.hawkbit.repository.jpa.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleMetadataRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SoftwareModuleManagement}.
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

    public JpaSoftwareModuleManagement(final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final SoftwareModuleRepository softwareModuleRepository,
            final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository, final AuditorAware<String> auditorProvider,
            final ArtifactManagement artifactManagement, final QuotaManagement quotaManagement,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final Database database) {
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
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public List<SoftwareModule> create(final Collection<SoftwareModuleCreate> swModules) {
        final List<JpaSoftwareModule> modulesToCreate = swModules.stream().map(JpaSoftwareModuleCreate.class::cast)
                .map(JpaSoftwareModuleCreate::build).toList();

        final List<SoftwareModule> createdModules = Collections
                .unmodifiableList(softwareModuleRepository.saveAll(AccessController.Operation.CREATE, modulesToCreate));

        if (createdModules.stream().anyMatch(SoftwareModule::isEncrypted)) {
            entityManager.flush();
            createdModules.stream().filter(SoftwareModule::isEncrypted).map(SoftwareModule::getId)
                    .forEach(encryptedModuleId -> ArtifactEncryptionService.getInstance()
                            .addSoftwareModuleEncryptionSecrets(encryptedModuleId));
        }
        return createdModules;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public SoftwareModule create(final SoftwareModuleCreate c) {
        final JpaSoftwareModuleCreate create = (JpaSoftwareModuleCreate) c;

        final JpaSoftwareModule sm = softwareModuleRepository.save(AccessController.Operation.CREATE, create.build());
        if (create.isEncrypted()) {
            // flush sm creation in order to get an Id
            entityManager.flush();
            ArtifactEncryptionService.getInstance().addSoftwareModuleEncryptionSecrets(sm.getId());
        }

        return sm;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public SoftwareModule update(final SoftwareModuleUpdate u) {
        final GenericSoftwareModuleUpdate update = (GenericSoftwareModuleUpdate) u;

        final JpaSoftwareModule module = softwareModuleRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, update.getId()));

        update.getDescription().ifPresent(module::setDescription);
        update.getVendor().ifPresent(module::setVendor);

        // lock/unlock ONLY if locked flag is present!
        if (Boolean.TRUE.equals(update.locked())) {
            module.lock();
        } else if (Boolean.FALSE.equals(update.locked())) {
            module.unlock();
        }

        return softwareModuleRepository.save(module);
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public long count() {
        return softwareModuleRepository.count(SoftwareModuleSpecification.isNotDeleted());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void delete(final long id) {
        delete(List.of(id));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void delete(final Collection<Long> ids) {
        final List<JpaSoftwareModule> swModulesToDelete = softwareModuleRepository.findAllById(ids);
        if (swModulesToDelete.size() < ids.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, ids,
                    swModulesToDelete.stream().map(SoftwareModule::getId).toList());
        }

        final Set<Long> assignedModuleIds = new HashSet<>();
        swModulesToDelete.forEach(swModule -> {
            // execute this count operation without access limitations since we have to
            // ensure it's not assigned when deleting it.
            if (distributionSetRepository.countByModulesId(swModule.getId()) <= 0) {
                softwareModuleRepository.deleteById(swModule.getId());
            } else {
                assignedModuleIds.add(swModule.getId());
            }
            // schedule delete binary data of artifacts
            deleteGridFsArtifacts(swModule);
        });

        if (!assignedModuleIds.isEmpty()) {
            String currentUser = null;
            if (auditorProvider != null) {
                currentUser = auditorProvider.getCurrentAuditor().orElse(null);
            }

            /*
               TODO AC - could use single update query as before via entity manager directly (considers tenant!):
               maybe it will be possible, via specification when migrate to Spring Boot 3
               "UPDATE JpaSoftwareModule b SET b.deleted = 1, b.lastModifiedAt = :lastModifiedAt, b.lastModifiedBy = :lastModifiedBy WHERE b.tenant = :tenant AND b.id IN :ids"
             */
            final long timestamp = System.currentTimeMillis();
            final List<JpaSoftwareModule> toDelete = softwareModuleRepository.findAll(
                    AccessController.Operation.DELETE, softwareModuleRepository.byIdsSpec(assignedModuleIds));
            for (final JpaSoftwareModule softwareModule : toDelete) {
                softwareModule.setDeleted(true);
                softwareModule.setLastModifiedAt(timestamp);
                softwareModule.setLastModifiedBy(currentUser);
            }
            softwareModuleRepository.saveAll(toDelete);
        }
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public List<SoftwareModule> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleRepository.findAllById(ids));
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public boolean exists(final long id) {
        return softwareModuleRepository.existsById(id);
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Optional<SoftwareModule> get(final long id) {
        return softwareModuleRepository.findById(id).map(SoftwareModule.class::cast);
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Slice<SoftwareModule> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, pageable, List.of(
                SoftwareModuleSpecification.isNotDeleted(),
                SoftwareModuleSpecification.fetchType()));
    }

    @Override
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<SoftwareModule> findByRsql(final Pageable pageable, final String rsqlParam) {
        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleRepository, pageable, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, SoftwareModuleFields.class, virtualPropertyReplacer,
                        database),
                SoftwareModuleSpecification.isNotDeleted()));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<SoftwareModuleMetadata> createMetaData(final Collection<SoftwareModuleMetadataCreate> create) {
        if (!create.isEmpty()) {
            // check if all metadata entries refer to the same software module
            final Long id = ((JpaSoftwareModuleMetadataCreate) create.iterator().next()).getSoftwareModuleId();
            if (createJpaMetadataCreateStream(create).allMatch(c -> id.equals(c.getSoftwareModuleId()))) {
                assertSoftwareModuleExists(id);
                assertMetaDataQuota(id, create.size());

                // touch to update revision and last modified timestamp
                JpaManagementHelper.touch(entityManager, softwareModuleRepository, (JpaSoftwareModule) get(id)
                        .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
                return createJpaMetadataCreateStream(create).map(this::saveMetadata).collect(Collectors.toList());
            } else {
                // group by software module id to minimize database access
                final Map<Long, List<JpaSoftwareModuleMetadataCreate>> groups = createJpaMetadataCreateStream(create)
                        .collect(Collectors.groupingBy(JpaSoftwareModuleMetadataCreate::getSoftwareModuleId));
                return groups.entrySet().stream().flatMap(e -> {
                    final List<JpaSoftwareModuleMetadataCreate> group = e.getValue();

                    assertSoftwareModuleExists(id);
                    assertMetaDataQuota(e.getKey(), group.size());

                    // touch to update revision and last modified timestamp
                    JpaManagementHelper.touch(entityManager, softwareModuleRepository, (JpaSoftwareModule) get(id)
                            .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
                    return group.stream().map(this::saveMetadata);
                }).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata createMetaData(final SoftwareModuleMetadataCreate c) {
        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;
        final Long id = create.getSoftwareModuleId();

        assertSoftwareModuleExists(id);
        assertMetaDataQuota(id, 1);

        // touch to update revision and last modified timestamp
        JpaManagementHelper.touch(entityManager, softwareModuleRepository, (JpaSoftwareModule) get(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
        return saveMetadata(create);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetaData(final long id, final String key) {
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(id,
                key).orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class, id, key));

        JpaManagementHelper.touch(entityManager, softwareModuleRepository,
                (JpaSoftwareModule) metadata.getSoftwareModule());
        softwareModuleMetadataRepository.deleteById(metadata.getId());
    }

    @Override
    public Page<SoftwareModule> findByAssignedTo(final Pageable pageable, final long distributionSetId) {
        assertDistributionSetExists(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleRepository, pageable,
                Collections.singletonList(SoftwareModuleSpecification.byAssignedToDs(distributionSetId)));
    }

    @Override
    public long countByAssignedTo(final long distributionSetId) {
        assertDistributionSetExists(distributionSetId);

        return softwareModuleRepository.count(SoftwareModuleSpecification.byAssignedToDs(distributionSetId));
    }

    @Override
    public Slice<SoftwareModule> findByTextAndType(final Pageable pageable, final String searchText,
            final Long typeId) {
        final List<Specification<JpaSoftwareModule>> specList = new ArrayList<>(3);
        specList.add(SoftwareModuleSpecification.isNotDeleted());

        if (!ObjectUtils.isEmpty(searchText)) {
            specList.add(buildSmSearchQuerySpec(searchText));
        }

        if (null != typeId) {
            assertSoftwareModuleTypeExists(typeId);
            specList.add(SoftwareModuleSpecification.equalType(typeId));
        }

        specList.add(SoftwareModuleSpecification.fetchType());

        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, pageable, specList);
    }

    @Override
    public Optional<SoftwareModule> getByNameAndVersionAndType(final String name, final String version,
            final long typeId) {
        assertSoftwareModuleTypeExists(typeId);

        // TODO AC - Access is restricted. This could have problem with UI when access control is enabled.
        // Vaadin UI use this for validation. May need to be called via elevated access
        return JpaManagementHelper
                .findOneBySpec(softwareModuleRepository, List.of(
                        SoftwareModuleSpecification.likeNameAndVersion(name, version),
                        SoftwareModuleSpecification.equalType(typeId),
                        SoftwareModuleSpecification.fetchType()))
                .map(SoftwareModule.class::cast);
    }

    @Override
    public Optional<SoftwareModuleMetadata> getMetaDataBySoftwareModuleId(final long id, final String key) {
        assertSoftwareModuleExists(id);

        return softwareModuleMetadataRepository.findById(new SwMetadataCompositeKey(id, key))
                .map(SoftwareModuleMetadata.class::cast);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleId(final Pageable pageable, final long id) {
        assertSoftwareModuleExists(id);

        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleMetadataRepository, pageable,
                Collections.singletonList(metadataBySoftwareModuleIdSpec(id)));
    }

    @Override
    public long countMetaDataBySoftwareModuleId(final long id) {
        assertSoftwareModuleExists(id);

        return softwareModuleMetadataRepository.countBySoftwareModuleId(id);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(final Pageable pageable,
            final long id) {
        assertSoftwareModuleExists(id);

        return JpaManagementHelper.convertPage(softwareModuleMetadataRepository.findBySoftwareModuleIdAndTargetVisible(
                id, true, PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT)), pageable);
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataByRsql(final Pageable pageable, final long id,
            final String rsqlParam) {
        assertSoftwareModuleExists(id);

        final List<Specification<JpaSoftwareModuleMetadata>> specList = Arrays
                .asList(RSQLUtility.buildRsqlSpecification(rsqlParam, SoftwareModuleMetadataFields.class,
                        virtualPropertyReplacer, database), metadataBySoftwareModuleIdSpec(id));
        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleMetadataRepository, pageable, specList);
    }

    @Override
    public Slice<SoftwareModule> findByType(final Pageable pageable, final long typeId) {
        assertSoftwareModuleTypeExists(typeId);

        return JpaManagementHelper.findAllWithoutCountBySpec(
                softwareModuleRepository,
                pageable,
                List.of(
                        SoftwareModuleSpecification.equalType(typeId),
                        SoftwareModuleSpecification.isNotDeleted()));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void lock(final long id) {
        final JpaSoftwareModule softwareModule = softwareModuleRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        if (!softwareModule.isLocked()) {
            softwareModule.lock();
            softwareModuleRepository.save(softwareModule);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void unlock(final long id) {
        final JpaSoftwareModule softwareModule = softwareModuleRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        if (softwareModule.isLocked()) {
            softwareModule.unlock();
            softwareModuleRepository.save(softwareModule);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetaData(final SoftwareModuleMetadataUpdate u) {
        final GenericSoftwareModuleMetadataUpdate update = (GenericSoftwareModuleMetadataUpdate) u;

        // check if exists otherwise throw entity not found exception
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) getMetaDataBySoftwareModuleId(
                update.getSoftwareModuleId(), update.getKey())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class, update.getSoftwareModuleId(), update.getKey()));

        update.getValue().ifPresent(metadata::setValue);
        update.isTargetVisible().ifPresent(metadata::setTargetVisible);

        JpaManagementHelper.touch(entityManager, softwareModuleRepository, metadata.getSoftwareModule());
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    public Map<Long, List<SoftwareModuleMetadata>> findMetaDataBySoftwareModuleIdsAndTargetVisible(final Collection<Long> moduleIds) {
        return softwareModuleMetadataRepository
                .findBySoftwareModuleIdInAndTargetVisible(moduleIds, true, PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT))
                .getContent().stream()
                .collect(Collectors.groupingBy(o -> (Long) o[0], Collectors.mapping(o -> (SoftwareModuleMetadata) o[1], Collectors.toList())));
    }

    private static Stream<JpaSoftwareModuleMetadataCreate> createJpaMetadataCreateStream(
            final Collection<SoftwareModuleMetadataCreate> create) {
        return create.stream().map(JpaSoftwareModuleMetadataCreate.class::cast);
    }

    private static Specification<JpaSoftwareModuleMetadata> metadataBySoftwareModuleIdSpec(final long id) {
        return (root, query, cb) -> cb
                .equal(root.get(JpaSoftwareModuleMetadata_.softwareModule).get(JpaSoftwareModule_.id), id);
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {
        softwareModuleRepository.getAccessController().ifPresent(accessController ->
                accessController.assertOperationAllowed(AccessController.Operation.DELETE, swModule));
        final Set<String> sha1Hashes = swModule.getArtifacts().stream().map(Artifact::getSha1Hash).collect(Collectors.toSet());
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() ->
                sha1Hashes.forEach(sha1Hash -> ((JpaArtifactManagement) artifactManagement).clearArtifactBinary(sha1Hash)));
    }

    private Specification<JpaSoftwareModule> buildSmSearchQuerySpec(final String searchText) {
        final String[] smFilterNameAndVersionEntries = JpaManagementHelper
                .getFilterNameAndVersionEntries(searchText.trim());
        return SoftwareModuleSpecification.likeNameAndVersion(smFilterNameAndVersionEntries[0],
                smFilterNameAndVersionEntries[1]);
    }

    private SoftwareModuleMetadata saveMetadata(final JpaSoftwareModuleMetadataCreate create) {
        assertSoftwareModuleMetadataDoesNotExist(create.getSoftwareModuleId(), create);
        return softwareModuleMetadataRepository.save(create.build());
    }

    private void assertSoftwareModuleMetadataDoesNotExist(final Long id,
            final JpaSoftwareModuleMetadataCreate md) {
        if (softwareModuleMetadataRepository.existsById(new SwMetadataCompositeKey(id, md.getKey()))) {
            throw new EntityAlreadyExistsException("Metadata entry with key '" + md.getKey() + "' already exists!");
        }
    }

    /**
     * Asserts the meta data quota for the software module with the given ID.
     *
     * @param id The software module ID.
     * @param requested Number of meta data entries to be created.
     */
    private void assertMetaDataQuota(final Long id, final int requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        QuotaHelper.assertAssignmentQuota(id, requested, maxMetaData, SoftwareModuleMetadata.class,
                SoftwareModule.class, softwareModuleMetadataRepository::countBySoftwareModuleId);
    }

    private void assertSoftwareModuleExists(final Long id) {
        if (!softwareModuleRepository.existsById(id)) {
            throw new EntityNotFoundException(SoftwareModule.class, id);
        }
    }

    private void assertSoftwareModuleTypeExists(final Long typeId) {
        if (!softwareModuleTypeRepository.existsById(typeId)) {
            throw new EntityNotFoundException(SoftwareModuleType.class, typeId);
        }
    }

    private void assertDistributionSetExists(final long distributionSetId) {
        if (!distributionSetRepository.existsById(distributionSetId)) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetId);
        }
    }
}
