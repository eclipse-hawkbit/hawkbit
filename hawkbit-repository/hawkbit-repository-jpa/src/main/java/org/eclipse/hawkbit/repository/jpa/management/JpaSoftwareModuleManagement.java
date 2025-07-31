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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule.JpaMetadataValue;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
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
        extends AbstractJpaRepositoryWithMetadataManagement<JpaSoftwareModule, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update, SoftwareModuleRepository, SoftwareModuleFields, MetadataValue, JpaSoftwareModule.JpaMetadataValue>
        implements SoftwareModuleManagement<JpaSoftwareModule> {

    private final DistributionSetRepository distributionSetRepository;
    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final ArtifactManagement artifactManagement;
    private final QuotaManagement quotaManagement;

    protected JpaSoftwareModuleManagement(
            final SoftwareModuleRepository softwareModuleRepository,
            final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final ArtifactManagement artifactManagement, final QuotaManagement quotaManagement) {
        super(softwareModuleRepository, entityManager);
        this.distributionSetRepository = distributionSetRepository;
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

    // called only with 'system code' access, so no need to check access control
    @Override
    public Map<Long, Map<String, String>> findMetaDataBySoftwareModuleIdsAndTargetVisible(final Collection<Long> ids) {
        return jpaRepository.findVisibleMetadataByModuleIds(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        entry -> (Long) entry[0],
                        Collectors.toMap(
                                entry -> (String) entry[1],
                                entry -> (String) entry[2],
                                (existing, replacement) -> existing, // in case of duplicates, keep the first one
                                HashMap::new)));
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
    public long countByAssignedTo(final long distributionSetId) {
        assertDistributionSetExists(distributionSetId);

        return jpaRepository.count(SoftwareModuleSpecification.byAssignedToDs(distributionSetId));
    }

    /**
     * Asserts the meta-data quota for the software module with the given ID.
     *
     * @param requested Number of meta-data entries to be created.
     */
    @Override
    protected void assertMetadataQuota(final long requested) {
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        QuotaHelper.assertAssignmentQuota(requested, maxMetaData, SoftwareModule.MetadataValueCreate.class, SoftwareModule.class);
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