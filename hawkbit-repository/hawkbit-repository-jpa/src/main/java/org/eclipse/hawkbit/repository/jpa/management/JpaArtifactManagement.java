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

import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.ArtifactStorage;
import org.eclipse.hawkbit.artifact.encryption.ArtifactEncryptionService;
import org.eclipse.hawkbit.artifact.exception.ArtifactBinaryNotFoundException;
import org.eclipse.hawkbit.artifact.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.artifact.exception.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.artifact.exception.HashNotMatchException;
import org.eclipse.hawkbit.artifact.model.ArtifactHashes;
import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.eclipse.hawkbit.artifact.model.StoredArtifactInfo;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.InvalidMd5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSha1HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSha256HashException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ArtifactSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeAndStorageQuotaCheckingInputStream;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.resilience.annotation.Retryable;
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

    private final ArtifactRepository artifactRepository;
    private final ArtifactStorage artifactStorage;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final EntityManager entityManager;
    private final PlatformTransactionManager txManager;
    private final QuotaManagement quotaManagement;

    protected JpaArtifactManagement(
            final ArtifactRepository artifactRepository,
            final Optional<ArtifactStorage> artifactStorage,
            final SoftwareModuleRepository softwareModuleRepository,
            final EntityManager entityManager,
            final PlatformTransactionManager txManager,
            final QuotaManagement quotaManagement) {
        this.artifactRepository = artifactRepository;
        this.artifactStorage = artifactStorage.orElse(null);
        this.softwareModuleRepository = softwareModuleRepository;
        this.entityManager = entityManager;
        this.txManager = txManager;
        this.quotaManagement = quotaManagement;
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public Artifact create(final ArtifactUpload artifactUpload) {
        if (artifactStorage == null) {
            throw new UnsupportedOperationException();
        }

        final long moduleId = artifactUpload.moduleId();
        QuotaHelper.assertAssignmentQuota(
                moduleId, 1, quotaManagement.getMaxArtifactsPerSoftwareModule(),
                Artifact.class, SoftwareModule.class,
                // get all artifacts without user context
                softwareModuleId -> artifactRepository.count(null, ArtifactSpecifications.bySoftwareModuleId(softwareModuleId)));

        final JpaSoftwareModule softwareModule = softwareModuleRepository.getById(moduleId);
        if (softwareModule.isLocked()) {
            // check in order to:
            // - on non-existing artifact - skip binary storing before, eventual, failing in new JpaSoftwareModule.addArtifact
            // - if existing and overriding - no check will be made in new JpaSoftwareModule.addArtifact, so we sh to fail here
            throw new LockedException(JpaSoftwareModule.class, softwareModule.getId(), "ADD_ARTIFACT");
        }

        final String filename = artifactUpload.filename();
        final Artifact existing = softwareModule.getArtifactByFilename(filename).orElse(null);
        if (existing != null) {
            if (artifactUpload.overrideExisting()) {
                log.debug("overriding existing artifact with new filename {}", filename);
            } else {
                throw new EntityAlreadyExistsException("File with that name already exists in the Software Module");
            }
        }

        // touch it to update the lock revision because we are modifying the DS indirectly, it will, also check UPDATE access
        JpaManagementHelper.touch(entityManager, softwareModuleRepository, softwareModule);

        final StoredArtifactInfo artifact = storeArtifact(artifactUpload, softwareModule.isEncrypted());
        try {
            return storeArtifactMetadata(softwareModule, filename, artifact.getHashes(), artifact.getSize(), existing);
        } catch (final Exception e) {
            artifactStorage.deleteBySha1(AccessContext.tenant(), artifact.getHashes().sha1());
            throw e;
        }
    }

    @SuppressWarnings("java:S2201") // java:S2201 - the idea is to just check if the artifact exists
    @Override
    public ArtifactStream getArtifactStream(final String sha1Hash, final long softwareModuleId, final boolean isEncrypted) {
        if (artifactStorage == null) {
            throw new UnsupportedOperationException();
        }

        final String tenant = AccessContext.tenant();
        // check access to the software module and if artifact belongs to it
        for (final Artifact artifact : softwareModuleRepository.getById(softwareModuleId).getArtifacts()) {
            if (artifact.getSha1Hash().equals(sha1Hash)) {
                if (isEncrypted) {
                    final ArtifactEncryptionService encryptionService = ArtifactEncryptionService.getInstance();
                    return new ArtifactStream(
                            encryptionService.decryptArtifact(softwareModuleId, artifactStorage.getBySha1(tenant, sha1Hash)),
                            artifact.getSize() - encryptionService.encryptionSizeOverhead(),
                            artifact.getSha1Hash());
                } else {
                    return new ArtifactStream(artifactStorage.getBySha1(tenant, sha1Hash), artifact.getSize(), artifact.getSha1Hash());
                }
            }
        }
        throw new ArtifactBinaryNotFoundException(sha1Hash);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void delete(final long id) {
        if (artifactStorage == null) {
            throw new UnsupportedOperationException();
        }

        final JpaArtifact toDelete = artifactRepository.getById(id);

        final JpaSoftwareModule softwareModule = toDelete.getSoftwareModule();
        // clearArtifactBinary checks (unconditionally) software module UPDATE access
        softwareModuleRepository.getAccessController().ifPresent(accessController ->
                accessController.assertOperationAllowed(AccessController.Operation.UPDATE, softwareModule));
        softwareModule.removeArtifact(toDelete);
        softwareModuleRepository.save(softwareModule);

        artifactRepository.deleteById(id);

        final String sha1Hash = toDelete.getSha1Hash();
        afterCommit(() -> clearArtifactBinary(sha1Hash));
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
            if (artifactRepository.countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(sha1Hash, AccessContext.tenant()) <= 0) {
                // removes the real artifact ONLY AFTER the delete of artifact or software module
                // in local history has passed successfully (caller has permission and no errors)
                afterCommit(() -> {
                    try {
                        log.debug("deleting artifact from repository {}", sha1Hash);
                        artifactStorage.deleteBySha1(AccessContext.tenant(), sha1Hash);
                    } catch (final ArtifactStoreException e) {
                        throw new ArtifactDeleteFailedException(e);
                    }
                });
            } // else there are still other artifacts that need the binary
            return null;
        });
    }

    private StoredArtifactInfo storeArtifact(final ArtifactUpload artifactUpload, final boolean isSmEncrypted) {
        final InputStream stream = artifactUpload.inputStream();
        try (final InputStream wrappedStream = wrapInQuotaStream(
                isSmEncrypted ? ArtifactEncryptionService.getInstance().encryptArtifact(artifactUpload.moduleId(), stream) : stream)) {
            return artifactStorage.store(
                    AccessContext.tenant(),
                    wrappedStream, artifactUpload.filename(),
                    artifactUpload.contentType(), artifactUpload.hash());
        } catch (final ArtifactStoreException | IOException e) {
            throw new ArtifactUploadFailedException(e);
        } catch (final HashNotMatchException e) {
            if (e.getHashFunction().equals(HashNotMatchException.SHA1)) {
                throw new InvalidSha1HashException(e.getMessage(), e);
            } else if (e.getHashFunction().equals(HashNotMatchException.SHA256)) {
                throw new InvalidSha256HashException(e.getMessage(), e);
            } else {
                throw new InvalidMd5HashException(e.getMessage(), e);
            }
        }
    }

    private InputStream wrapInQuotaStream(final InputStream in) {
        final long maxArtifactSize = quotaManagement.getMaxArtifactSize();

        final long currentlyUsed = artifactRepository.sumOfNonDeletedArtifactSize().orElse(0L);
        final long maxArtifactSizeTotal = quotaManagement.getMaxArtifactStorage();

        return new FileSizeAndStorageQuotaCheckingInputStream(in, maxArtifactSize, maxArtifactSizeTotal - currentlyUsed);
    }

    private Artifact storeArtifactMetadata(
            final JpaSoftwareModule softwareModule, final String providedFilename,
            final ArtifactHashes hash, final long fileSize,
            final Artifact existing) {
        final JpaArtifact artifact;
        if (existing == null) {
            artifact = new JpaArtifact(hash.sha1(), providedFilename, softwareModule);
            softwareModule.addArtifact(artifact);
        } else {
            artifact = (JpaArtifact) existing;
            artifact.setSha1Hash(hash.sha1());
        }
        artifact.setMd5Hash(hash.md5());
        artifact.setSha256Hash(hash.sha256());
        artifact.setFileSize(fileSize);

        log.debug("storing new artifact into repository {}", artifact);
        return artifactRepository.save(AccessController.Operation.CREATE, artifact);
    }
}