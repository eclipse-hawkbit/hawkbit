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

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.artifact.ArtifactRepository;
import org.eclipse.hawkbit.repository.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.repository.artifact.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.artifact.exception.HashNotMatchException;
import org.eclipse.hawkbit.repository.artifact.model.AbstractDbArtifact;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifact;
import org.eclipse.hawkbit.repository.artifact.model.DbArtifactHash;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA256HashException;
import org.eclipse.hawkbit.repository.jpa.EncryptionAwareDbArtifact;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ArtifactSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeAndStorageQuotaCheckingInputStream;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA based {@link ArtifactManagement} implementation.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "artifact-management" }, matchIfMissing = true)
public class JpaArtifactManagement implements ArtifactManagement {

    private final LocalArtifactRepository localArtifactRepository;
    private final ArtifactRepository artifactRepository;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager txManager;
    private final TenantAware tenantAware;
    private final QuotaManagement quotaManagement;

    protected JpaArtifactManagement(
            final LocalArtifactRepository localArtifactRepository,
            final Optional<ArtifactRepository> artifactRepository,
            final SoftwareModuleRepository softwareModuleRepository,
            final EntityManager entityManager,
            final PlatformTransactionManager txManager,
            final QuotaManagement quotaManagement,
            final TenantAware tenantAware) {
        this.localArtifactRepository = localArtifactRepository;
        this.artifactRepository = artifactRepository.orElse(null);
        this.softwareModuleRepository = softwareModuleRepository;
        this.entityManager = entityManager;
        this.txManager = txManager;
        this.quotaManagement = quotaManagement;
        this.tenantAware = tenantAware;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifact create(final ArtifactUpload artifactUpload) {
        if (artifactRepository == null) {
            throw new UnsupportedOperationException();
        }

        final long moduleId = artifactUpload.moduleId();
        QuotaHelper.assertAssignmentQuota(
                moduleId, 1, quotaManagement.getMaxArtifactsPerSoftwareModule(),
                Artifact.class, SoftwareModule.class,
                // get all artifacts without user context
                softwareModuleId -> localArtifactRepository.count(null, ArtifactSpecifications.bySoftwareModuleId(softwareModuleId)));

        final JpaSoftwareModule softwareModule = softwareModuleRepository.getById(moduleId);

        final String filename = artifactUpload.filename();
        final Artifact existing = softwareModule.getArtifactByFilename(filename).orElse(null);
        if (existing != null) {
            if (artifactUpload.overrideExisting()) {
                log.debug("overriding existing artifact with new filename {}", filename);
            } else {
                throw new EntityAlreadyExistsException("File with that name already exists in the Software Module");
            }
        }

        // touch it to update the lock revision because we are modifying the
        // DS indirectly, it will, also check UPDATE access
        JpaManagementHelper.touch(entityManager, softwareModuleRepository, softwareModule);

        final AbstractDbArtifact artifact = storeArtifact(artifactUpload, softwareModule.isEncrypted());
        try {
            return storeArtifactMetadata(softwareModule, filename, artifact, existing);
        } catch (final Exception e) {
            artifactRepository.deleteBySha1(tenantAware.getCurrentTenant(), artifact.getHashes().sha1());
            throw e;
        }
    }

    @SuppressWarnings("java:S2201") // java:S2201 - the idea is to just check if the artifact exists
    @Override
    public DbArtifact loadArtifactBinary(final String sha1Hash, final long softwareModuleId, final boolean isEncrypted) {
        if (artifactRepository == null) {
            throw new UnsupportedOperationException();
        }

        final String tenant = tenantAware.getCurrentTenant();
        // check access to the software module and if artifact belongs to it
        for (final Artifact artifact : softwareModuleRepository.getById(softwareModuleId).getArtifacts()) {
            if (artifact.getSha1Hash().equals(sha1Hash)) {
                final DbArtifact dbArtifact = artifactRepository.getBySha1(tenant, sha1Hash);
                return isEncrypted ? wrapInEncryptionAwareDbArtifact(softwareModuleId, dbArtifact) : dbArtifact;
            }
        }
        throw new ArtifactBinaryNotFoundException(sha1Hash);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        if (artifactRepository == null) {
            throw new UnsupportedOperationException();
        }

        final JpaArtifact toDelete = localArtifactRepository.getById(id);

        final JpaSoftwareModule softwareModule = toDelete.getSoftwareModule();
        // clearArtifactBinary checks (unconditionally) software module UPDATE access
        softwareModuleRepository.getAccessController().ifPresent(accessController ->
                accessController.assertOperationAllowed(AccessController.Operation.UPDATE, softwareModule));
        softwareModule.removeArtifact(toDelete);
        softwareModuleRepository.save(softwareModule);

        localArtifactRepository.deleteById(id);

        final String sha1Hash = toDelete.getSha1Hash();
        AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() -> clearArtifactBinary(sha1Hash));
    }

    /**
     * Garbage collects artifact binaries if only referenced by given {@link SoftwareModule#getId()} or {@link SoftwareModule}'s that are
     * marked as deleted.
     * <p/>
     * Software module related UPDATE permission shall be checked by the callers!
     * <p/>
     * Note: Internal method. Shall be called ONLY if @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
     * has already been checked
     *
     * @param sha1Hash no longer needed
     */
    void clearArtifactBinary(final String sha1Hash) {
        DeploymentHelper.runInNewTransaction(txManager, "clearArtifactBinary", status -> {
            // countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse will skip ACM checks and will return total count as it should be
            if (localArtifactRepository.countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(sha1Hash,
                    tenantAware.getCurrentTenant()) <= 0) { // 1 artifact is the one being deleted!
                // removes the real artifact ONLY AFTER the delete of artifact or software module
                // in local history has passed successfully (caller has permission and no errors)
                AfterTransactionCommitExecutorHolder.getInstance().getAfterCommit().afterCommit(() -> {
                    try {
                        log.debug("deleting artifact from repository {}", sha1Hash);
                        artifactRepository.deleteBySha1(tenantAware.getCurrentTenant(), sha1Hash);
                    } catch (final ArtifactStoreException e) {
                        throw new ArtifactDeleteFailedException(e);
                    }
                });
            } // else there are still other artifacts that need the binary
            return null;
        });
    }

    private AbstractDbArtifact storeArtifact(final ArtifactUpload artifactUpload, final boolean isSmEncrypted) {
        final InputStream stream = artifactUpload.inputStream();
        try (final InputStream wrappedStream = wrapInQuotaStream(
                isSmEncrypted ? wrapInEncryptionStream(artifactUpload.moduleId(), stream) : stream)) {
            return artifactRepository.store(
                    tenantAware.getCurrentTenant(), wrappedStream, artifactUpload.filename(), artifactUpload.contentType(),
                    new DbArtifactHash(artifactUpload.providedSha1Sum(), artifactUpload.providedMd5Sum(), artifactUpload.providedSha256Sum()));
        } catch (final ArtifactStoreException | IOException e) {
            throw new ArtifactUploadFailedException(e);
        } catch (final HashNotMatchException e) {
            if (e.getHashFunction().equals(HashNotMatchException.SHA1)) {
                throw new InvalidSHA1HashException(e.getMessage(), e);
            } else if (e.getHashFunction().equals(HashNotMatchException.SHA256)) {
                throw new InvalidSHA256HashException(e.getMessage(), e);
            } else {
                throw new InvalidMD5HashException(e.getMessage(), e);
            }
        }
    }

    private InputStream wrapInEncryptionStream(final long smId, final InputStream stream) {
        return ArtifactEncryptionService.getInstance().encryptArtifact(smId, stream);
    }

    private InputStream wrapInQuotaStream(final InputStream in) {
        final long maxArtifactSize = quotaManagement.getMaxArtifactSize();

        final long currentlyUsed = localArtifactRepository.sumOfNonDeletedArtifactSize().orElse(0L);
        final long maxArtifactSizeTotal = quotaManagement.getMaxArtifactStorage();

        return new FileSizeAndStorageQuotaCheckingInputStream(in, maxArtifactSize, maxArtifactSizeTotal - currentlyUsed);
    }

    private DbArtifact wrapInEncryptionAwareDbArtifact(final long softwareModuleId, final DbArtifact dbArtifact) {
        if (dbArtifact == null) {
            return null;
        }
        final ArtifactEncryptionService encryptionService = ArtifactEncryptionService.getInstance();
        return new EncryptionAwareDbArtifact(dbArtifact,
                stream -> encryptionService.decryptArtifact(softwareModuleId, stream),
                encryptionService.encryptionSizeOverhead());
    }

    private Artifact storeArtifactMetadata(
            final SoftwareModule softwareModule, final String providedFilename, final AbstractDbArtifact result,
            final Artifact existing) {
        final JpaArtifact artifact;
        if (existing == null) {
            artifact = new JpaArtifact(result.getHashes().sha1(), providedFilename, softwareModule);
        } else {
            artifact = (JpaArtifact) existing;
            artifact.setSha1Hash(result.getHashes().sha1());
        }
        artifact.setMd5Hash(result.getHashes().md5());
        artifact.setSha256Hash(result.getHashes().sha256());
        artifact.setFileSize(result.getSize());

        log.debug("storing new artifact into repository {}", artifact);
        return localArtifactRepository.save(AccessController.Operation.CREATE, artifact);
    }
}