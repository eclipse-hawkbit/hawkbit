/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.repository.HashNotMatchException;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.repository.ArtifactEncryptionService;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA256HashException;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.specifications.ArtifactSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeAndStorageQuotaCheckingInputStream;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA based {@link ArtifactManagement} implementation.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaArtifactManagement implements ArtifactManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaArtifactManagement.class);

    private final LocalArtifactRepository localArtifactRepository;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final ArtifactRepository artifactRepository;

    private final TenantAware tenantAware;

    private final QuotaManagement quotaManagement;
    private final AccessController<JpaArtifact> artifactAccessController;
    private final AccessController<JpaSoftwareModule> softwareModuleAccessController;

    JpaArtifactManagement(final LocalArtifactRepository localArtifactRepository,
            final SoftwareModuleRepository softwareModuleRepository, final ArtifactRepository artifactRepository,
            final QuotaManagement quotaManagement, final TenantAware tenantAware,
            final AccessController<JpaArtifact> artifactAccessController,
            final AccessController<JpaSoftwareModule> softwareModuleAccessController) {
        this.localArtifactRepository = localArtifactRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.artifactRepository = artifactRepository;
        this.quotaManagement = quotaManagement;
        this.tenantAware = tenantAware;
        this.artifactAccessController = artifactAccessController;
        this.softwareModuleAccessController = softwareModuleAccessController;
    }

    private static Artifact checkForExistingArtifact(final String filename, final boolean overrideExisting,
                                                     final SoftwareModule softwareModule) {
        final Optional<Artifact> artifact = softwareModule.getArtifactByFilename(filename);

        if (artifact.isPresent()) {
            if (overrideExisting) {
                LOG.debug("overriding existing artifact with new filename {}", filename);
                return artifact.get();
            } else {
                throw new EntityAlreadyExistsException("File with that name already exists in the Software Module");
            }
        }
        return null;
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifact create(final ArtifactUpload artifactUpload) {
        final long moduleId = artifactUpload.getModuleId();
        final JpaSoftwareModule softwareModule =
                softwareModuleRepository
                        .findOne(softwareModuleAccessController.appendAccessRules(
                                        AccessController.Operation.READ, SoftwareModuleSpecification.byId(moduleId)))
                        .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId));

        softwareModuleAccessController.assertOperationAllowed(AccessController.Operation.UPDATE, softwareModule);

        assertArtifactQuota(moduleId, 1);

        final String filename = artifactUpload.getFilename();
        final Artifact existing = checkForExistingArtifact(filename, artifactUpload.overrideExisting(), softwareModule);

        final AbstractDbArtifact artifact = storeArtifact(artifactUpload, softwareModule.isEncrypted());
        try {
            return storeArtifactMetadata(softwareModule, filename, artifact, existing);
        } catch (final InsufficientPermissionException e) {
            artifactRepository.deleteBySha1(tenantAware.getCurrentTenant(), artifact.getHashes().getSha1());
            throw e;
        }
    }

    private AbstractDbArtifact storeArtifact(final ArtifactUpload artifactUpload, final boolean isSmEncrypted) {
        final String tenant = tenantAware.getCurrentTenant();
        final long smId = artifactUpload.getModuleId();
        final InputStream stream = artifactUpload.getInputStream();
        final String fileName = artifactUpload.getFilename();
        final String contentType = artifactUpload.getContentType();
        final String providedSha1 = artifactUpload.getProvidedSha1Sum();
        final String providedMd5 = artifactUpload.getProvidedMd5Sum();
        final String providedSha256 = artifactUpload.getProvidedSha256Sum();

        try (final InputStream wrappedStream = wrapInQuotaStream(
                isSmEncrypted ? wrapInEncryptionStream(smId, stream) : stream)) {
            return artifactRepository.store(tenant, wrappedStream, fileName, contentType,
                    new DbArtifactHash(providedSha1, providedMd5, providedSha256));
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
        return ArtifactEncryptionService.getInstance().encryptSoftwareModuleArtifact(smId, stream);
    }

    private void assertArtifactQuota(final long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxArtifactsPerSoftwareModule(),
                Artifact.class, SoftwareModule.class, localArtifactRepository::countBySoftwareModuleId);
    }

    private InputStream wrapInQuotaStream(final InputStream in) {
        final long maxArtifactSize = quotaManagement.getMaxArtifactSize();

        final long currentlyUsed = localArtifactRepository.getSumOfUndeletedArtifactSize().orElse(0L);
        final long maxArtifactSizeTotal = quotaManagement.getMaxArtifactStorage();

        return new FileSizeAndStorageQuotaCheckingInputStream(in, maxArtifactSize,
                maxArtifactSizeTotal - currentlyUsed);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public boolean clearArtifactBinary(final String sha1Hash, final long softwareModuleId) {
        // unconditional check of software module id access is essential, used by #delete(int)
        verifyModuleAccess(AccessController.Operation.DELETE, softwareModuleId);

        final long count = localArtifactRepository.countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(
            sha1Hash,
            tenantAware.getCurrentTenant());
        if (count > 1) { // 1 artifact is the one being deleted!
            // there are still other artifacts that need the binary
            return false;
        }

        // test if the one meta that remains is for that software module,
        // and have access oon check access
        if (artifactAccessController.isOperationAllowed(
                AccessController.Operation.DELETE,
                () -> findFirstBySHA1(sha1Hash)
                        .map(JpaArtifact.class::cast)
                        // else there is no artifact for that software module,
                        // throws InsufficientPermissionException in order to
                        // make result of the test false -> won't remove
                        .orElseThrow(InsufficientPermissionException::new))) {
            try {
                LOG.debug("deleting artifact from repository {}", sha1Hash);
                // TODO - if the transaction fail the artifact will, anyway, be deleted,
                // TODO - could be thought about moving it after the root transaction
                artifactRepository.deleteBySha1(tenantAware.getCurrentTenant(), sha1Hash);
                return true;
            } catch (final ArtifactStoreException e) {
                throw new ArtifactDeleteFailedException(e);
            }
        } else {
            return false;
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        final JpaArtifact existing = (JpaArtifact) get(id)
                .orElseThrow(() -> new EntityNotFoundException(Artifact.class, id));

        // clearArtifactBinary checks (unconditionally) software module DELETE access
        clearArtifactBinary(existing.getSha1Hash(), existing.getSoftwareModule().getId());
        // validate artifact access, check could have been skipped in the clearArtifactBinary
        artifactAccessController.assertOperationAllowed(AccessController.Operation.DELETE, existing);

        ((JpaSoftwareModule) existing.getSoftwareModule()).removeArtifact(existing);
        softwareModuleRepository.save((JpaSoftwareModule) existing.getSoftwareModule());
        localArtifactRepository.deleteById(id);
    }

    @Override
    public Optional<Artifact> get(final long id) {
        final Specification<JpaArtifact> specification = artifactAccessController
                .appendAccessRules(AccessController.Operation.READ, ArtifactSpecification.byId(id));

        return localArtifactRepository.findOne(specification).map(Artifact.class::cast);
    }

    @Override
    public Optional<Artifact> getByFilenameAndSoftwareModule(final String filename, final long softwareModuleId) {
        // verifies the software module access
        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        return localArtifactRepository.findFirstByFilenameAndSoftwareModuleId(filename, softwareModuleId)
                .filter(artifact ->
                        artifactAccessController
                                .isOperationAllowed(AccessController.Operation.READ, (JpaArtifact)artifact));
    }

    @Override
    public Optional<Artifact> findFirstBySHA1(final String sha1Hash) {
        // validate artifact access
        final Specification<JpaArtifact> specification = artifactAccessController
                .appendAccessRules(AccessController.Operation.READ, ArtifactSpecification.bySha1(sha1Hash));

        // find first entity matching sha1
        final List<JpaArtifact> content = localArtifactRepository
                .findAllWithoutCount(specification, PageRequest.of(0, 1)).getContent();
        return firstWithReadPermissions(content);
    }

    @Override
    public Optional<Artifact> getByFilename(final String filename) {
        final Specification<JpaArtifact> specification = artifactAccessController
                .appendAccessRules(AccessController.Operation.READ, ArtifactSpecification.byFilename(filename));

        // find first entity matching sha1
        final List<JpaArtifact> content = localArtifactRepository
                .findAllWithoutCount(specification, PageRequest.of(0, 1)).getContent();
        return firstWithReadPermissions(content);
    }

    @Override
    public Page<Artifact> findBySoftwareModule(final Pageable pageReq, final long softwareModuleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        // TODO AC - apply AC for artifacts
        return localArtifactRepository.findBySoftwareModuleId(pageReq, softwareModuleId);
    }

    @Override
    public long countBySoftwareModule(final long softwareModuleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        // TODO AC - apply AC for artifacts, or remove any checks for AC
        return localArtifactRepository.countBySoftwareModuleId(softwareModuleId);
    }

    @Override
    public long count() {
        // TODO AC - apply AC for artifacts & software modules, or remove any checks for AC
        return localArtifactRepository.count(artifactAccessController.getAccessRules(AccessController.Operation.READ));
    }

    @Override
    public Optional<DbArtifact> loadArtifactBinary(final String sha1Hash, final long softwareModuleId,
            final boolean isEncrypted) {
        verifyModuleAccess(AccessController.Operation.READ, softwareModuleId);

        final String tenant = tenantAware.getCurrentTenant();
        if (artifactRepository.existsByTenantAndSha1(tenant, sha1Hash)) {
            artifactAccessController.assertOperationAllowed(
                    AccessController.Operation.READ,
                    () -> findFirstBySHA1(sha1Hash)
                            .map(JpaArtifact.class::cast)
                            .filter(artifact -> softwareModuleId == artifact.getSoftwareModule().getId())
                            // if not found no assertOperationAllowed shall fail
                            .orElseThrow(InsufficientPermissionException::new));
            final DbArtifact dbArtifact = artifactRepository.getArtifactBySha1(tenant, sha1Hash);
            return Optional.ofNullable(
                    isEncrypted ? wrapInEncryptionAwareDbArtifact(softwareModuleId, dbArtifact) : dbArtifact);
        }

        return Optional.empty();
    }

    private DbArtifact wrapInEncryptionAwareDbArtifact(final long softwareModuleId, final DbArtifact dbArtifact) {
        if (dbArtifact == null) {
            return null;
        }
        final ArtifactEncryptionService encryptionService = ArtifactEncryptionService.getInstance();
        return new EncryptionAwareDbArtifact(dbArtifact,
                stream -> encryptionService.decryptSoftwareModuleArtifact(softwareModuleId, stream),
                encryptionService.encryptionSizeOverhead());
    }

    private Artifact storeArtifactMetadata(final SoftwareModule softwareModule, final String providedFilename,
            final AbstractDbArtifact result, final Artifact existing) {
        final JpaArtifact artifact;
        if (existing == null) {
            artifact = new JpaArtifact(result.getHashes().getSha1(), providedFilename, softwareModule);
        } else {
            artifact = (JpaArtifact) existing;
            artifact.setSha1Hash(result.getHashes().getSha1());
        }
        artifact.setMd5Hash(result.getHashes().getMd5());
        artifact.setSha256Hash(result.getHashes().getSha256());
        artifact.setSize(result.getSize());
        artifactAccessController.assertOperationAllowed(AccessController.Operation.CREATE, artifact);

        LOG.debug("storing new artifact into repository {}", artifact);
        return localArtifactRepository.save(artifact);
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long softwareModuleId) {
        final Specification<JpaSoftwareModule> specification = softwareModuleAccessController
                .appendAccessRules(AccessController.Operation.READ, SoftwareModuleSpecification.byId(softwareModuleId));

        if (!softwareModuleRepository.exists(specification)) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
        }
    }

    private void verifyModuleAccess(final AccessController.Operation operation, final long moduleId) {
        softwareModuleAccessController.assertOperationAllowed(
                operation,
                () -> softwareModuleRepository
                        .findById(moduleId)
                        .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, moduleId)));
    }

    private Optional<Artifact> firstWithReadPermissions(final List<JpaArtifact> artifactList) {
        if (!artifactList.isEmpty()) {
            for (final JpaArtifact artifact : artifactList) {
                try {
                    // check access to the software module the found artefact belongs to
                    softwareModuleAccessController.assertOperationAllowed(
                            AccessController.Operation.READ, (JpaSoftwareModule) artifact.getSoftwareModule());
                    return Optional.of(artifact);
                } catch (final InsufficientPermissionException e) {
                    // no access to software module - so not to artifact, returns empty
                }
            }
        }
        return Optional.empty();
    }
}
