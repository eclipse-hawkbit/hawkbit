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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
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
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata_;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SoftwareModuleManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaSoftwareModuleManagement implements SoftwareModuleManagement {

    protected static final String SOFTWARE_MODULE_METADATA = "SoftwareModuleMetadata";
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

    @SuppressWarnings("java:S107")
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
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        delete0(List.of(id));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> ids) {
        delete0(ids);
    }
    private void delete0(final Collection<Long> ids) {
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
    public Optional<SoftwareModule> get(final long id) {
        return softwareModuleRepository.findById(id).map(SoftwareModule.class::cast);
    }

    @Override
    public List<SoftwareModule> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(softwareModuleRepository.findAllById(ids));
    }

    @Override
    public boolean exists(final long id) {
        return softwareModuleRepository.existsById(id);
    }

    @Override
    public long count() {
        return softwareModuleRepository.count(SoftwareModuleSpecification.isNotDeleted());
    }

    @Override
    public Slice<SoftwareModule> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, List.of(
                SoftwareModuleSpecification.isNotDeleted(),
                SoftwareModuleSpecification.fetchType()), pageable);
    }

    @Override
    public Page<SoftwareModule> findByRsql(final String rsql, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleRepository, List.of(
                RSQLUtility.buildRsqlSpecification(rsql, SoftwareModuleFields.class, virtualPropertyReplacer,
                        database),
                SoftwareModuleSpecification.isNotDeleted()), pageable);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void createMetadata(final Collection<SoftwareModuleMetadataCreate> create) {
        // group by software module id to minimize database access
        create.stream()
                .map(JpaSoftwareModuleMetadataCreate.class::cast)
                .collect(Collectors.groupingBy(JpaSoftwareModuleMetadataCreate::getSoftwareModuleId))
                .forEach((id, createsForSoftwareModule) -> {
                    assertSoftwareModuleExists(id);
                    assertMetadataQuota(id, createsForSoftwareModule.size());

                    // touch to update revision and last modified timestamp
                    JpaManagementHelper.touch(
                            entityManager, softwareModuleRepository,
                            (JpaSoftwareModule) get(id).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
                    createsForSoftwareModule.forEach(this::saveMetadata);
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<SoftwareModuleMetadata> getMetadata(final long id) {
        assertSoftwareModuleExists(id);

        return (List)softwareModuleMetadataRepository.findAll(metadataBySoftwareModuleIdSpec(id));
    }

    @Override
    public SoftwareModuleMetadata getMetadata(final long id, final String key) {
        assertSoftwareModuleExists(id);

        return findMetadata(id, key).orElseThrow(() -> new EntityNotFoundException(SOFTWARE_MODULE_METADATA, id + ":" + key));
    }

    @Override
    public Page<SoftwareModuleMetadata> findMetaDataBySoftwareModuleIdAndTargetVisible(final long id, final Pageable pageable) {
        assertSoftwareModuleExists(id);

        return JpaManagementHelper.convertPage(softwareModuleMetadataRepository.findBySoftwareModuleIdAndTargetVisible(
                id, true, PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT)), pageable);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetadata(final SoftwareModuleMetadataCreate c) {
        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;
        final Long id = create.getSoftwareModuleId();

        assertSoftwareModuleExists(id);
        assertMetadataQuota(id, 1);

        // touch to update revision and last modified timestamp
        JpaManagementHelper.touch(
                entityManager, softwareModuleRepository,
                (JpaSoftwareModule) get(id).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
        return saveMetadata(create);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetadata(final SoftwareModuleMetadataUpdate u) {
        final GenericSoftwareModuleMetadataUpdate update = (GenericSoftwareModuleMetadataUpdate) u;

        // check if exists otherwise throw entity not found exception
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) findMetadata(
                update.getSoftwareModuleId(), update.getKey())
                .orElseThrow(() -> new EntityNotFoundException(SOFTWARE_MODULE_METADATA, update.getSoftwareModuleId() + ":" + update.getKey()));

        update.getValue().ifPresent(metadata::setValue);
        update.isTargetVisible().ifPresent(metadata::setTargetVisible);

        JpaManagementHelper.touch(entityManager, softwareModuleRepository, metadata.getSoftwareModule());
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetadata(final long id, final String key) {
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) findMetadata(id, key)
                .orElseThrow(() -> new EntityNotFoundException(SOFTWARE_MODULE_METADATA, id + ":" + key));

        JpaManagementHelper.touch(entityManager, softwareModuleRepository, metadata.getSoftwareModule());
        softwareModuleMetadataRepository.deleteById(metadata.getId());
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
    public Page<SoftwareModule> findByAssignedTo(final long distributionSetId, final Pageable pageable) {
        assertDistributionSetExists(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(softwareModuleRepository,
                Collections.singletonList(SoftwareModuleSpecification.byAssignedToDs(distributionSetId)), pageable
        );
    }

    @Override
    public Slice<SoftwareModule> findByTextAndType(final String searchText, final Long typeId, final Pageable pageable) {
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

        return JpaManagementHelper.findAllWithoutCountBySpec(softwareModuleRepository, specList, pageable);
    }

    @Override
    public Optional<SoftwareModule> findByNameAndVersionAndType(final String name, final String version,
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
    public Slice<SoftwareModule> findByType(final long typeId, final Pageable pageable) {
        assertSoftwareModuleTypeExists(typeId);

        return JpaManagementHelper.findAllWithoutCountBySpec(
                softwareModuleRepository,
                List.of(
                        SoftwareModuleSpecification.equalType(typeId),
                        SoftwareModuleSpecification.isNotDeleted()), pageable
        );
    }

    @Override
    public Map<Long, List<SoftwareModuleMetadata>> findMetaDataBySoftwareModuleIdsAndTargetVisible(final Collection<Long> moduleIds) {
        return softwareModuleMetadataRepository
                .findBySoftwareModuleIdInAndTargetVisible(moduleIds, true, PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT))
                .getContent().stream()
                .collect(Collectors.groupingBy(o -> (Long) o[0], Collectors.mapping(o -> (SoftwareModuleMetadata) o[1], Collectors.toList())));
    }

    @Override
    public long countByAssignedTo(final long distributionSetId) {
        assertDistributionSetExists(distributionSetId);

        return softwareModuleRepository.count(SoftwareModuleSpecification.byAssignedToDs(distributionSetId));
    }

    private static Specification<JpaSoftwareModuleMetadata> metadataBySoftwareModuleIdSpec(final long id) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleMetadata_.softwareModule).get(AbstractJpaBaseEntity_.id), id);
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {
        softwareModuleRepository.getAccessController().ifPresent(accessController ->
                accessController.assertOperationAllowed(AccessController.Operation.DELETE, swModule));
        final Set<String> sha1Hashes = swModule.getArtifacts().stream().map(Artifact::getSha1Hash).collect(Collectors.toSet());
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() ->
                sha1Hashes.forEach(((JpaArtifactManagement) artifactManagement)::clearArtifactBinary));
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

    private Optional<SoftwareModuleMetadata> findMetadata(final long id, final String key) {
        assertSoftwareModuleExists(id);

        return softwareModuleMetadataRepository.findById(new SwMetadataCompositeKey(id, key))
                .map(SoftwareModuleMetadata.class::cast);
    }

    private void assertSoftwareModuleMetadataDoesNotExist(final Long id, final JpaSoftwareModuleMetadataCreate md) {
        if (softwareModuleMetadataRepository.existsById(new SwMetadataCompositeKey(id, md.getKey()))) {
            throw new EntityAlreadyExistsException("Metadata entry with key '" + md.getKey() + "' already exists!");
        }
    }

    /**
     * Asserts the meta-data quota for the software module with the given ID.
     *
     * @param id The software module ID.
     * @param requested Number of meta-data entries to be created.
     */
    private void assertMetadataQuota(final Long id, final int requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        QuotaHelper.assertAssignmentQuota(
                id, requested, maxMetaData, SoftwareModuleMetadata.class, SoftwareModule.class,
                softwareModuleMetadataRepository::countBySoftwareModuleId);
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
