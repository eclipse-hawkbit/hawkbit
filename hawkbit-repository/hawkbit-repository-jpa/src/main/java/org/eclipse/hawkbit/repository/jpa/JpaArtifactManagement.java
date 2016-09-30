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
import java.util.List;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.repository.HashNotMatchException;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.GridFSDBFileNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.jpa.model.JpaExternalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaExternalArtifactProvider;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.model.ExternalArtifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA based {@link ArtifactManagement} implementation.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaArtifactManagement implements ArtifactManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaArtifactManagement.class);

    @Autowired
    private LocalArtifactRepository localArtifactRepository;

    @Autowired
    private ExternalArtifactRepository externalArtifactRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private ExternalArtifactProviderRepository externalArtifactProviderRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    private static LocalArtifact checkForExistingArtifact(final String filename, final boolean overrideExisting,
            final SoftwareModule softwareModule) {
        if (softwareModule.getLocalArtifactByFilename(filename).isPresent()) {
            if (overrideExisting) {
                LOG.debug("overriding existing artifact with new filename {}", filename);
                return softwareModule.getLocalArtifactByFilename(filename).get();
            } else {
                throw new EntityAlreadyExistsException("File with that name already exists in the Software Module");
            }
        }
        return null;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public ExternalArtifact createExternalArtifact(final ExternalArtifactProvider externalRepository,
            final String urlSuffix, final Long moduleId) {

        final SoftwareModule module = getModuleAndThrowExceptionIfThatFails(moduleId);
        return externalArtifactRepository.save(new JpaExternalArtifact(externalRepository, urlSuffix, module));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public ExternalArtifactProvider createExternalArtifactProvider(final String name, final String description,
            final String basePath, final String defaultUrlSuffix) {
        return externalArtifactProviderRepository
                .save(new JpaExternalArtifactProvider(name, description, basePath, defaultUrlSuffix));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public LocalArtifact createLocalArtifact(final InputStream stream, final Long moduleId, final String filename,
            final String providedMd5Sum, final String providedSha1Sum, final boolean overrideExisting,
            final String contentType) {
        DbArtifact result = null;

        final SoftwareModule softwareModule = getModuleAndThrowExceptionIfThatFails(moduleId);

        final LocalArtifact existing = checkForExistingArtifact(filename, overrideExisting, softwareModule);

        try {
            result = artifactRepository.store(stream, filename, contentType,
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

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteExternalArtifact(final Long id) {
        final ExternalArtifact existing = externalArtifactRepository.findOne(id);

        if (null == existing) {
            return;
        }

        existing.getSoftwareModule().removeArtifact(existing);
        softwareModuleRepository.save((JpaSoftwareModule) existing.getSoftwareModule());
        externalArtifactRepository.delete(id);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteLocalArtifact(final LocalArtifact existing) {
        if (existing == null) {
            return;
        }

        boolean artifactIsOnlyUsedByOneSoftwareModule = true;
        for (final LocalArtifact lArtifact : localArtifactRepository
                .findByGridFsFileName(((JpaLocalArtifact) existing).getGridFsFileName())) {
            if (!lArtifact.getSoftwareModule().isDeleted()
                    && Long.compare(lArtifact.getSoftwareModule().getId(), existing.getSoftwareModule().getId()) != 0) {
                artifactIsOnlyUsedByOneSoftwareModule = false;
                break;
            }
        }

        if (artifactIsOnlyUsedByOneSoftwareModule) {
            try {
                LOG.debug("deleting artifact from repository {}", ((JpaLocalArtifact) existing).getGridFsFileName());
                artifactRepository.deleteBySha1(((JpaLocalArtifact) existing).getGridFsFileName());
            } catch (final ArtifactStoreException e) {
                throw new ArtifactDeleteFailedException(e);
            }
        }
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteLocalArtifact(final Long id) {
        final JpaLocalArtifact existing = localArtifactRepository.findOne(id);

        if (null == existing) {
            return;
        }

        deleteLocalArtifact(existing);

        existing.getSoftwareModule().removeArtifact(existing);
        softwareModuleRepository.save((JpaSoftwareModule) existing.getSoftwareModule());
        localArtifactRepository.delete(id);
    }

    @Override
    public LocalArtifact findLocalArtifact(final Long id) {
        return localArtifactRepository.findOne(id);
    }

    @Override
    public List<LocalArtifact> findByFilenameAndSoftwareModule(final String filename, final Long softwareModuleId) {
        return localArtifactRepository.findByFilenameAndSoftwareModuleId(filename, softwareModuleId);
    }

    @Override
    public LocalArtifact findFirstLocalArtifactsBySHA1(final String sha1) {
        return localArtifactRepository.findFirstByGridFsFileName(sha1);
    }

    @Override
    public List<LocalArtifact> findLocalArtifactByFilename(final String filename) {
        return localArtifactRepository.findByFilename(filename);
    }

    @Override
    public Page<LocalArtifact> findLocalArtifactBySoftwareModule(final Pageable pageReq, final Long swId) {
        return localArtifactRepository.findBySoftwareModuleId(pageReq, swId);
    }

    @Override
    public SoftwareModule findSoftwareModuleById(final Long id) {

        final Specification<JpaSoftwareModule> spec = SoftwareModuleSpecification.byId(id);

        return softwareModuleRepository.findOne(spec);
    }

    @Override
    public SoftwareModule findSoftwareModuleWithDetails(final Long id) {
        final SoftwareModule result = findSoftwareModuleById(id);
        if (result != null) {
            result.getArtifacts().size();
        }

        return result;
    }

    private SoftwareModule getModuleAndThrowExceptionIfThatFails(final Long moduleId) {
        final SoftwareModule softwareModule = findSoftwareModuleWithDetails(moduleId);

        if (softwareModule == null) {
            LOG.debug("no software module with ID {} exists", moduleId);
            throw new EntityNotFoundException("Software Module: " + moduleId);
        }
        return softwareModule;
    }

    @Override
    public DbArtifact loadLocalArtifactBinary(final LocalArtifact artifact) {
        final DbArtifact result = artifactRepository
                .getArtifactBySha1(((JpaLocalArtifact) artifact).getGridFsFileName());
        if (result == null) {
            throw new GridFSDBFileNotFoundException(((JpaLocalArtifact) artifact).getGridFsFileName());
        }

        return result;
    }

    private LocalArtifact storeArtifactMetadata(final SoftwareModule softwareModule, final String providedFilename,
            final DbArtifact result, final LocalArtifact existing) {
        JpaLocalArtifact artifact = (JpaLocalArtifact) existing;
        if (existing == null) {
            artifact = new JpaLocalArtifact(result.getHashes().getSha1(), providedFilename, softwareModule);
        }
        artifact.setMd5Hash(result.getHashes().getMd5());
        artifact.setSha1Hash(result.getHashes().getSha1());
        artifact.setSize(result.getSize());

        LOG.debug("storing new artifact into repository {}", artifact);
        return localArtifactRepository.save(artifact);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, null);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting, final String contentType) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, contentType);
    }

    @Override
    public Long countLocalArtifactsAll() {
        return localArtifactRepository.count();
    }

    @Override
    public Long countExternalArtifactsAll() {
        return externalArtifactRepository.count();
    }
}
