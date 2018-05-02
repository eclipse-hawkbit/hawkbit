/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.InputStream;
import java.util.Optional;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.repository.HashNotMatchException;
import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.exception.QuotaExceededException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private static final String MAX_ARTIFACT_SIZE_EXCEEDED = "Quota exceeded: The artifact '%s' (%s bytes) which has been uploaded for software module '%s' exceeds the maximum artifact size of %s bytes.";

    private final LocalArtifactRepository localArtifactRepository;

    private final SoftwareModuleRepository softwareModuleRepository;

    private final ArtifactRepository artifactRepository;

    private final TenantAware tenantAware;

    private final QuotaManagement quotaManagement;

    JpaArtifactManagement(final LocalArtifactRepository localArtifactRepository,
            final SoftwareModuleRepository softwareModuleRepository, final ArtifactRepository artifactRepository,
            final QuotaManagement quotaManagement, final TenantAware tenantAware) {
        this.localArtifactRepository = localArtifactRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.artifactRepository = artifactRepository;
        this.quotaManagement = quotaManagement;
        this.tenantAware = tenantAware;
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
    public Artifact create(final InputStream stream, final long moduleId, final String filename,
            final String providedMd5Sum, final String providedSha1Sum, final boolean overrideExisting,
            final String contentType, final long filesize) {
        AbstractDbArtifact result = null;

        final SoftwareModule softwareModule = getModuleAndThrowExceptionIfThatFails(moduleId);

        final Artifact existing = checkForExistingArtifact(filename, overrideExisting, softwareModule);

        assertArtifactQuota(moduleId, 1);
        assertMaxArtifactSizeQuota(filename, moduleId, filesize);

        try {
            result = artifactRepository.store(tenantAware.getCurrentTenant(), stream, filename, contentType,
                    new DbArtifactHash(providedSha1Sum, providedMd5Sum));
        } catch (final ArtifactStoreException e) {
            throw new ArtifactUploadFailedException(e);
        } catch (final HashNotMatchException e) {
            if (e.getHashFunction().equals(HashNotMatchException.SHA1)) {
                throw new InvalidSHA1HashException(e.getMessage(), e);
            } else {
                throw new InvalidMD5HashException(e.getMessage(), e);
            }
        }
        if (result == null) {
            return null;
        }

        return storeArtifactMetadata(softwareModule, filename, result, existing);
    }

    private void assertArtifactQuota(final long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxArtifactsPerSoftwareModule(),
                Artifact.class, SoftwareModule.class, localArtifactRepository::countBySoftwareModuleId);
    }

    private void assertMaxArtifactSizeQuota(final String filename, final long id, final long artifactSize) {
        final long maxArtifactSize = quotaManagement.getMaxArtifactSize();
        if (maxArtifactSize <= 0) {
            return;
        }
        if (artifactSize > maxArtifactSize) {
            final String msg = String.format(MAX_ARTIFACT_SIZE_EXCEEDED, filename, artifactSize, id, maxArtifactSize);
            LOG.warn(msg);
            throw new QuotaExceededException(msg);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public boolean clearArtifactBinary(final String sha1Hash, final long moduleId) {

        if (localArtifactRepository.existsWithSha1HashAndSoftwareModuleIdIsNot(sha1Hash, moduleId)) {
            // there are still other artifacts that need the binary
            return false;
        }

        try {
            LOG.debug("deleting artifact from repository {}", sha1Hash);
            artifactRepository.deleteBySha1(tenantAware.getCurrentTenant(), sha1Hash);
            return true;
        } catch (final ArtifactStoreException e) {
            throw new ArtifactDeleteFailedException(e);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        final JpaArtifact existing = (JpaArtifact) get(id)
                .orElseThrow(() -> new EntityNotFoundException(Artifact.class, id));

        clearArtifactBinary(existing.getSha1Hash(), existing.getSoftwareModule().getId());

        ((JpaSoftwareModule) existing.getSoftwareModule()).removeArtifact(existing);
        softwareModuleRepository.save((JpaSoftwareModule) existing.getSoftwareModule());
        localArtifactRepository.delete(id);
    }

    @Override
    public Optional<Artifact> get(final long id) {
        return Optional.ofNullable(localArtifactRepository.findOne(id));
    }

    @Override
    public Optional<Artifact> getByFilenameAndSoftwareModule(final String filename, final long softwareModuleId) {
        throwExceptionIfSoftwareModuleDoesNotExist(softwareModuleId);

        return localArtifactRepository.findFirstByFilenameAndSoftwareModuleId(filename, softwareModuleId);
    }

    @Override
    public Optional<Artifact> findFirstBySHA1(final String sha1Hash) {
        return localArtifactRepository.findFirstBySha1Hash(sha1Hash);
    }

    @Override
    public Optional<Artifact> getByFilename(final String filename) {
        return localArtifactRepository.findFirstByFilename(filename);
    }

    @Override
    public Page<Artifact> findBySoftwareModule(final Pageable pageReq, final long swId) {
        throwExceptionIfSoftwareModuleDoesNotExist(swId);

        return localArtifactRepository.findBySoftwareModuleId(pageReq, swId);
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long swId) {
        if (!softwareModuleRepository.exists(swId)) {
            throw new EntityNotFoundException(SoftwareModule.class, swId);
        }
    }

    @Override
    public Optional<AbstractDbArtifact> loadArtifactBinary(final String sha1Hash) {
        return Optional.ofNullable(artifactRepository.getArtifactBySha1(tenantAware.getCurrentTenant(), sha1Hash));
    }

    private Artifact storeArtifactMetadata(final SoftwareModule softwareModule, final String providedFilename,
            final AbstractDbArtifact result, final Artifact existing) {
        JpaArtifact artifact = (JpaArtifact) existing;
        if (existing == null) {
            artifact = new JpaArtifact(result.getHashes().getSha1(), providedFilename, softwareModule);
        }
        artifact.setMd5Hash(result.getHashes().getMd5());
        artifact.setSha1Hash(result.getHashes().getSha1());
        artifact.setSize(result.getSize());

        LOG.debug("storing new artifact into repository {}", artifact);
        return localArtifactRepository.save(artifact);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifact create(final InputStream inputStream, final long moduleId, final String filename,
            final boolean overrideExisting, final long filesize) {
        return create(inputStream, moduleId, filename, null, null, overrideExisting, null, filesize);
    }

    @Override
    public long count() {
        return localArtifactRepository.count();
    }

    private SoftwareModule getModuleAndThrowExceptionIfThatFails(final Long moduleId) {
        final SoftwareModule softwareModule = softwareModuleRepository.findOne(moduleId);

        if (softwareModule == null) {
            LOG.debug("no software module with ID {} exists", moduleId);
            throw new EntityNotFoundException(SoftwareModule.class, moduleId);
        }
        return softwareModule;
    }

}
