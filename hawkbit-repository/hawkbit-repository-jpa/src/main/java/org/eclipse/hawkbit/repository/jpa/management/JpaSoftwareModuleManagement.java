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

import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_DELAY;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleMetadataCreate;
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
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "software-module-management" }, matchIfMissing = true)
public class JpaSoftwareModuleManagement
        extends AbstractJpaRepositoryManagement<JpaSoftwareModule, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update, SoftwareModuleRepository, SoftwareModuleFields>
        implements SoftwareModuleManagement<JpaSoftwareModule> {

    protected static final String SOFTWARE_MODULE_METADATA = "SoftwareModuleMetadata";

    private final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement;
    private final DistributionSetRepository distributionSetRepository;
    private final SoftwareModuleMetadataRepository softwareModuleMetadataRepository;
    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final ArtifactManagement artifactManagement;
    private final QuotaManagement quotaManagement;

    protected JpaSoftwareModuleManagement(
            final SoftwareModuleRepository softwareModuleRepository,
            final EntityManager entityManager,
            final SoftwareModuleTypeManagement<? extends SoftwareModuleType> softwareModuleTypeManagement,
            final DistributionSetRepository distributionSetRepository,
            final SoftwareModuleMetadataRepository softwareModuleMetadataRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final ArtifactManagement artifactManagement, final QuotaManagement quotaManagement) {
        super(softwareModuleRepository, entityManager);
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetRepository = distributionSetRepository;
        this.softwareModuleMetadataRepository = softwareModuleMetadataRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.artifactManagement = artifactManagement;
        this.quotaManagement = quotaManagement;
    }

    @Override
    public List<JpaSoftwareModule> create(final Collection<Create> create) {
        final List<JpaSoftwareModule> createdModules = super.create(create);

        if (createdModules.stream().anyMatch(SoftwareModule::isEncrypted)) {
            // flush sm creation in order to get ids
            entityManager.flush();
            createdModules.stream()
                    .filter(SoftwareModule::isEncrypted)
                    .map(SoftwareModule::getId)
                    .forEach(encryptedModuleId -> ArtifactEncryptionService.getInstance().addEncryptionSecrets(encryptedModuleId));
        }

        return createdModules;
    }

    @Override
    public JpaSoftwareModule create(final Create create) {
        final JpaSoftwareModule createdModule = super.create(create);

        if (createdModule.isEncrypted()) {
            // flush sm creation in order to get an id
            entityManager.flush();
            ArtifactEncryptionService.getInstance().addEncryptionSecrets(createdModule.getId());
        }

        return createdModule;
    }

    @Override
    public JpaSoftwareModule update(final Update update) {
        final JpaSoftwareModule module = jpaRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, update.getId()));

        // lock/unlock ONLY if locked flag is present!
        if (Boolean.TRUE.equals(update.getLocked())) {
            module.lock();
        } else if (Boolean.FALSE.equals(update.getLocked())) {
            module.unlock();
        }

        return super.update(update, module);
    }

    @Override
    protected List<JpaSoftwareModule> softDelete(final Collection<JpaSoftwareModule> toDelete) {
        return toDelete.stream()
                .filter(swModule -> {
                    final List<DistributionSet> assignedTo = swModule.getAssignedTo();
                    if (assignedTo != null) {
                        final List<DistributionSet> lockedDS = assignedTo.stream()
                                .filter(DistributionSet::isLocked)
                                .filter(ds -> !ds.isDeleted())
                                .toList();
                        if (!lockedDS.isEmpty()) {
                            final StringBuilder sb = new StringBuilder("Part of ");
                            if (lockedDS.size() == 1) {
                                sb.append("a locked distribution set: ");
                            } else {
                                sb.append(lockedDS.size()).append(" locked distribution sets: ");
                            }
                            for (final DistributionSet ds : lockedDS) {
                                sb.append(ds.getName()).append(":").append(ds.getVersion()).append(" (").append(ds.getId()).append("), ");
                            }
                            sb.delete(sb.length() - 2, sb.length());
                            throw new LockedException(JpaSoftwareModule.class, swModule.getId(), "DELETE", sb.toString());
                        }
                    }
                    final boolean isAssigned = !ObjectUtils.isEmpty(assignedTo);
                    // schedule delete binary data of artifacts for every soft or not soft deleted module
                    deleteGridFsArtifacts(swModule);
                    return isAssigned;
                })
                .toList();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
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
                            entityManager, jpaRepository, jpaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
                    createsForSoftwareModule.forEach(this::saveMetadata);
                });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<SoftwareModuleMetadata> getMetadata(final long id) {
        assertSoftwareModuleExists(id);
        return (List) softwareModuleMetadataRepository.findAll(metadataBySoftwareModuleIdSpec(id));
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
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetadata(final SoftwareModuleMetadataCreate c) {
        final JpaSoftwareModuleMetadataCreate create = (JpaSoftwareModuleMetadataCreate) c;
        final Long id = create.getSoftwareModuleId();

        assertSoftwareModuleExists(id);
        assertMetadataQuota(id, 1);

        // touch to update revision and last modified timestamp
        JpaManagementHelper.touch(
                entityManager, jpaRepository, jpaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id)));
        return saveMetadata(create);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public SoftwareModuleMetadata updateMetadata(final SoftwareModuleMetadataUpdate u) {
        final GenericSoftwareModuleMetadataUpdate update = (GenericSoftwareModuleMetadataUpdate) u;

        // check if exists otherwise throw entity not found exception
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) findMetadata(
                update.getSoftwareModuleId(), update.getKey())
                .orElseThrow(() -> new EntityNotFoundException(SOFTWARE_MODULE_METADATA, update.getSoftwareModuleId() + ":" + update.getKey()));

        update.getValue().ifPresent(metadata::setValue);
        update.isTargetVisible().ifPresent(metadata::setTargetVisible);

        JpaManagementHelper.touch(entityManager, jpaRepository, metadata.getSoftwareModule());
        return softwareModuleMetadataRepository.save(metadata);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void deleteMetadata(final long id, final String key) {
        final JpaSoftwareModuleMetadata metadata = (JpaSoftwareModuleMetadata) findMetadata(id, key)
                .orElseThrow(() -> new EntityNotFoundException(SOFTWARE_MODULE_METADATA, id + ":" + key));

        JpaManagementHelper.touch(entityManager, jpaRepository, metadata.getSoftwareModule());
        softwareModuleMetadataRepository.deleteById(metadata.getId());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void lock(final long id) {
        final JpaSoftwareModule softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        if (!softwareModule.isLocked()) {
            softwareModule.lock();
            jpaRepository.save(softwareModule);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public void unlock(final long id) {
        final JpaSoftwareModule softwareModule = jpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, id));
        if (softwareModule.isLocked()) {
            softwareModule.unlock();
            jpaRepository.save(softwareModule);
        }
    }

    @Override
    public Page<JpaSoftwareModule> findByAssignedTo(final long distributionSetId, final Pageable pageable) {
        assertDistributionSetExists(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(
                jpaRepository,
                Collections.singletonList(SoftwareModuleSpecification.byAssignedToDs(distributionSetId)),
                pageable);
    }

    @Override
    public Slice<JpaSoftwareModule> findByTextAndType(final String searchText, final Long typeId, final Pageable pageable) {
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

        return JpaManagementHelper.findAllWithoutCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Optional<JpaSoftwareModule> findByNameAndVersionAndType(final String name, final String version, final long typeId) {
        assertSoftwareModuleTypeExists(typeId);

        // TODO AC - Access is restricted. This could have problem with UI when access control is enabled.
        // Vaadin UI use this for validation. May need to be called via elevated access
        return JpaManagementHelper
                .findOneBySpec(
                        jpaRepository,
                        List.of(
                                SoftwareModuleSpecification.likeNameAndVersion(name, version),
                                SoftwareModuleSpecification.equalType(typeId),
                                SoftwareModuleSpecification.fetchType()));
    }

    @Override
    public Slice<JpaSoftwareModule> findByType(final long typeId, final Pageable pageable) {
        assertSoftwareModuleTypeExists(typeId);

        return JpaManagementHelper.findAllWithoutCountBySpec(
                jpaRepository,
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

        return jpaRepository.count(SoftwareModuleSpecification.byAssignedToDs(distributionSetId));
    }

    private static Specification<JpaSoftwareModuleMetadata> metadataBySoftwareModuleIdSpec(final long id) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleMetadata_.softwareModule).get(AbstractJpaBaseEntity_.id), id);
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {
        jpaRepository.getAccessController().ifPresent(accessController ->
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
        if (!jpaRepository.existsById(id)) {
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