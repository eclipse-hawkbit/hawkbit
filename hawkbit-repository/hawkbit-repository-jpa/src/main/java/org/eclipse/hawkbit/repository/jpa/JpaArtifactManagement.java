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
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    private static Artifact checkForExistingArtifact(final String filename, final boolean overrideExisting,
            final SoftwareModule softwareModule) {
        if (softwareModule.getArtifactByFilename(filename).isPresent()) {
            if (overrideExisting) {
                LOG.debug("overriding existing artifact with new filename {}", filename);
                return softwareModule.getArtifactByFilename(filename).get();
            } else {
                throw new EntityAlreadyExistsException("File with that name already exists in the Software Module");
            }
        }
        return null;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Artifact createArtifact(final InputStream stream, final Long moduleId, final String filename,
            final String providedMd5Sum, final String providedSha1Sum, final boolean overrideExisting,
            final String contentType) {
        DbArtifact result = null;

        final SoftwareModule softwareModule = getModuleAndThrowExceptionIfThatFails(moduleId);

        final Artifact existing = checkForExistingArtifact(filename, overrideExisting, softwareModule);

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
    public boolean clearArtifactBinary(final Artifact existing) {

        for (final Artifact lArtifact : localArtifactRepository
                .findByGridFsFileName(((JpaArtifact) existing).getGridFsFileName())) {
            if (!lArtifact.getSoftwareModule().isDeleted()
                    && Long.compare(lArtifact.getSoftwareModule().getId(), existing.getSoftwareModule().getId()) != 0) {
                return false;
            }
        }

        try {
            LOG.debug("deleting artifact from repository {}", ((JpaArtifact) existing).getGridFsFileName());
            artifactRepository.deleteBySha1(((JpaArtifact) existing).getGridFsFileName());
            return true;
        } catch (final ArtifactStoreException e) {
            throw new ArtifactDeleteFailedException(e);
        }
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteArtifact(final Long id) {
        final JpaArtifact existing = localArtifactRepository.findOne(id);

        if (null == existing) {
            return;
        }

        clearArtifactBinary(existing);

        ((JpaSoftwareModule) existing.getSoftwareModule()).removeArtifact(existing);
        softwareModuleRepository.save((JpaSoftwareModule) existing.getSoftwareModule());
        localArtifactRepository.delete(id);
    }

    @Override
    public Artifact findArtifact(final Long id) {
        return localArtifactRepository.findOne(id);
    }

    @Override
    public List<Artifact> findByFilenameAndSoftwareModule(final String filename, final Long softwareModuleId) {
        return localArtifactRepository.findByFilenameAndSoftwareModuleId(filename, softwareModuleId);
    }

    @Override
    public Artifact findFirstArtifactBySHA1(final String sha1) {
        return localArtifactRepository.findFirstByGridFsFileName(sha1);
    }

    @Override
    public List<Artifact> findArtifactByFilename(final String filename) {
        return localArtifactRepository.findByFilename(filename);
    }

    @Override
    public Page<Artifact> findArtifactBySoftwareModule(final Pageable pageReq, final Long swId) {
        return localArtifactRepository.findBySoftwareModuleId(pageReq, swId);
    }

    @Override
    public DbArtifact loadArtifactBinary(final Artifact artifact) {
        final DbArtifact result = artifactRepository.getArtifactBySha1(((JpaArtifact) artifact).getGridFsFileName());
        if (result == null) {
            throw new GridFSDBFileNotFoundException(((JpaArtifact) artifact).getGridFsFileName());
        }

        return result;
    }

    private Artifact storeArtifactMetadata(final SoftwareModule softwareModule, final String providedFilename,
            final DbArtifact result, final Artifact existing) {
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
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Artifact createArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting) {
        return createArtifact(inputStream, moduleId, filename, null, null, overrideExisting, null);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Artifact createArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting, final String contentType) {
        return createArtifact(inputStream, moduleId, filename, null, null, overrideExisting, contentType);
    }

    @Override
    public Long countArtifactsAll() {
        return localArtifactRepository.count();
    }

    private SoftwareModule getModuleAndThrowExceptionIfThatFails(final Long moduleId) {
        final SoftwareModule softwareModule = softwareModuleRepository.findOne(moduleId);

        if (softwareModule == null) {
            LOG.debug("no software module with ID {} exists", moduleId);
            throw new EntityNotFoundException("Software Module: " + moduleId);
        }
        return softwareModule;
    }

}
