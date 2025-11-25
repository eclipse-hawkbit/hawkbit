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
import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteSoftwareModuleException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "software-module-management" }, matchIfMissing = true)
public class JpaSoftwareModuleManagement extends
        AbstractJpaRepositoryWithMetadataManagement<JpaSoftwareModule, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update, SoftwareModuleRepository, SoftwareModuleFields, MetadataValue, JpaSoftwareModule.JpaMetadataValue>
        implements SoftwareModuleManagement<JpaSoftwareModule> {

    private final DistributionSetRepository distributionSetRepository;
    private final ArtifactManagement artifactManagement;
    private final QuotaManagement quotaManagement;

    protected JpaSoftwareModuleManagement(final SoftwareModuleRepository softwareModuleRepository, final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository, final ArtifactManagement artifactManagement,
            final QuotaManagement quotaManagement) {
        super(softwareModuleRepository, entityManager);
        this.distributionSetRepository = distributionSetRepository;
        this.artifactManagement = artifactManagement;
        this.quotaManagement = quotaManagement;
    }

    @Override
    public List<JpaSoftwareModule> create(final Collection<Create> create) {
        final List<JpaSoftwareModule> createdModules = super.create(create);

        if (createdModules.stream().anyMatch(SoftwareModule::isEncrypted)) {
            // flush sm creation in order to get ids
            entityManager.flush();
            createdModules.stream().filter(SoftwareModule::isEncrypted).map(SoftwareModule::getId)
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
    protected List<JpaSoftwareModule> softDelete(final Collection<JpaSoftwareModule> toDelete) {
        return toDelete.stream().filter(swModule -> {
            final List<DistributionSet> assignedTo = swModule.getAssignedTo();
            if (assignedTo != null) {
                final List<DistributionSet> lockedDS = assignedTo.stream().filter(DistributionSet::isLocked).filter(ds -> !ds.isDeleted())
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
        }).toList();
    }

    // called only with 'system code' access, so no need to check access control
    @Override
    public Map<Long, Map<String, String>> findMetaDataBySoftwareModuleIdsAndTargetVisible(final Collection<Long> ids) {
        return jpaRepository.findVisibleMetadataByModuleIds(ids).stream().collect(Collectors.groupingBy(entry -> (Long) entry[0],
                Collectors.toMap(entry -> (String) entry[1], entry -> (String) entry[2], (existing, replacement) -> existing,
                        // in case of duplicates, keep the first one
                        HashMap::new)));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaSoftwareModule lock(final SoftwareModule softwareModule) {
        final JpaSoftwareModule jpaSoftwareModule = toJpaSoftwareModule(softwareModule);
        if (jpaSoftwareModule.isLocked()) {
            return jpaSoftwareModule;
        } else {
            if (!softwareModule.isComplete()) {
                throw new IncompleteSoftwareModuleException("Could not be locked while incomplete!");
            }
            jpaSoftwareModule.lock();
            return jpaRepository.save(jpaSoftwareModule);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = TX_RT_MAX, backoff = @Backoff(delay = TX_RT_DELAY))
    public JpaSoftwareModule unlock(final SoftwareModule softwareModule) {
        final JpaSoftwareModule jpaSoftwareModule = toJpaSoftwareModule(softwareModule);
        if (softwareModule.isLocked()) {
            jpaSoftwareModule.unlock();
            return jpaRepository.save(jpaSoftwareModule);
        } else {
            return jpaSoftwareModule;
        }
    }

    @Override
    public Page<JpaSoftwareModule> findByAssignedTo(final long distributionSetId, final Pageable pageable) {
        assertDistributionSetExists(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository,
                Collections.singletonList(SoftwareModuleSpecification.byAssignedToDs(distributionSetId)), pageable);
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

    private JpaSoftwareModule toJpaSoftwareModule(final SoftwareModule softwareModule) {
        if (softwareModule instanceof JpaSoftwareModule jpaSoftwareModule) {
            return jpaSoftwareModule;
        } else {
            return jpaRepository.getById(softwareModule.getId());
        }
    }

    private void deleteGridFsArtifacts(final JpaSoftwareModule swModule) {
        jpaRepository.getAccessController()
                .ifPresent(accessController -> accessController.assertOperationAllowed(AccessController.Operation.DELETE, swModule));
        final Set<String> sha1Hashes = swModule.getArtifacts().stream().map(Artifact::getSha1Hash).collect(Collectors.toSet());
        afterCommit(() -> sha1Hashes.forEach(((JpaArtifactManagement) artifactManagement)::clearArtifactBinary));
    }

    private void assertDistributionSetExists(final long distributionSetId) {
        if (!distributionSetRepository.existsById(distributionSetId)) {
            throw new EntityNotFoundException(DistributionSet.class, distributionSetId);
        }
    }
}