/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.io.InputStream;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.artifact.repository.ArtifactStoreException;
import org.eclipse.hawkbit.artifact.repository.HashNotMatchException;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.GridFSDBFileNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifact;
import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.specifications.SoftwareModuleSpecification;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.hateoas.Identifiable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * service for {@link Artifact} management operations.
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
@Validated
@Service
public class ArtifactManagement {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManagement.class);

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

    /**
     * Persists artifact binary as provided by given InputStream. assign the
     * artifact in addition to given {@link SoftwareModule}.
     *
     * @param stream
     *            to read from for artifact binary
     * @param moduleId
     *            to assign the new artifact to
     * @param filename
     *            of the artifact
     * @param providedSha1Sum
     *            optional sha1 checksum to check the new file against
     * @param providedMd5Sum
     *            optional md5 checksum to check the new file against
     * @param overrideExisting
     *            to <code>true</code> if the artifact binary can be overdiden
     *            if it already exists
     * @param contentType
     *            the contentType of the file
     * @return uploaded {@link LocalArtifact}
     *
     * @throws EntityNotFoundException
     *             if given software module does not exist
     * @throws EntityAlreadyExistsException
     *             if File with that name already exists in the Software Module
     * @throws ArtifactUploadFailedException
     *             if upload fails with internal server errors
     * @throws InvalidMD5HashException
     *             if check against provided MD5 checksum failed
     * @throws InvalidSHA1HashException
     *             if check against provided SHA1 checksum failed
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public LocalArtifact createLocalArtifact(@NotNull final InputStream stream, @NotNull final Long moduleId,
            @NotEmpty final String filename, final String providedMd5Sum, final String providedSha1Sum,
            final boolean overrideExisting, final String contentType) {
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

    private LocalArtifact checkForExistingArtifact(final String filename, final boolean overrideExisting,
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

    private SoftwareModule getModuleAndThrowExceptionIfThatFails(final Long moduleId) {
        final SoftwareModule softwareModule = findSoftwareModuleWithDetails(moduleId);

        if (softwareModule == null) {
            LOG.debug("no software module with ID {} exists", moduleId);
            throw new EntityNotFoundException("Software Module: " + moduleId);
        }
        return softwareModule;
    }

    /**
     * Retrieves software module including details (
     * {@link SoftwareModule#getArtifacts()}).
     *
     * @param id
     *            parameter
     * @param isDeleted
     *            parameter
     * @return the found {@link SoftwareModule}s
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    protected SoftwareModule findSoftwareModuleWithDetails(@NotNull final Long id) {
        final SoftwareModule result = findSoftwareModuleById(id);
        if (result != null) {
            result.getArtifacts().size();
        }

        return result;
    }

    /**
     * Find all local artifact by sha1 and return the first artifact.
     *
     * @param sha1
     *            the sha1
     * @return the first local artifact
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public LocalArtifact findFirstLocalArtifactsBySHA1(final String sha1) {
        return localArtifactRepository.findFirstByGridFsFileName(sha1);
    }

    /**
     * Finds {@link SoftwareModule} by given id.
     *
     * @param id
     *            to search for
     * @return the found {@link SoftwareModule}s or <code>null</code> if not
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    protected SoftwareModule findSoftwareModuleById(@NotNull final Long id) {

        final Specification<SoftwareModule> spec = SoftwareModuleSpecification.byId(id);

        return softwareModuleRepository.findOne(spec);
    }

    private LocalArtifact storeArtifactMetadata(final SoftwareModule softwareModule, final String providedFilename,
            final DbArtifact result, final LocalArtifact existing) {
        LocalArtifact artifact = existing;
        if (existing == null) {
            artifact = new LocalArtifact(result.getHashes().getSha1(), providedFilename, softwareModule);
        }
        artifact.setMd5Hash(result.getHashes().getMd5());
        artifact.setSha1Hash(result.getHashes().getSha1());
        artifact.setSize(result.getSize());

        LOG.debug("storing new artifact into repository {}", artifact);
        final LocalArtifact artifactPersisted = localArtifactRepository.save(artifact);

        return artifactPersisted;
    }

    /**
     * Persists {@link ExternalArtifactProvider} based on given properties.
     *
     * @param name
     *            of the provided
     * @param description
     *            which is optional
     * @param basePath
     *            of all {@link ExternalArtifact}s of the provider
     * @param defaultUrlSuffix
     *            that is used if {@link ExternalArtifact#getUrlSuffix()} is
     *            empty.
     * @return created {@link ExternalArtifactProvider}
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public ExternalArtifactProvider createExternalArtifactProvider(@NotEmpty final String name,
            final String description, @NotNull final String basePath, final String defaultUrlSuffix) {
        return externalArtifactProviderRepository
                .save(new ExternalArtifactProvider(name, description, basePath, defaultUrlSuffix));
    }

    /**
     * Creates {@link ExternalArtifact} based on given provider.
     *
     * @param externalRepository
     *            the artifact is located in
     * @param urlSuffix
     *            of the artifact
     *            {@link ExternalArtifactProvider#getDefaultSuffix()} is used if
     *            empty.
     * @param moduleId
     *            to assign the artifact to
     *
     * @return created {@link ExternalArtifact}
     *
     * @throws EntityNotFoundException
     *             if {@link SoftwareModule} with given ID does not exist
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    public ExternalArtifact createExternalArtifact(@NotNull final ExternalArtifactProvider externalRepository,
            final String urlSuffix, @NotNull final Long moduleId) {

        final SoftwareModule module = getModuleAndThrowExceptionIfThatFails(moduleId);
        final ExternalArtifact result = externalArtifactRepository
                .save(new ExternalArtifact(externalRepository, urlSuffix, module));

        return result;
    }

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id
     *            of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException
     *             if deletion failed (MongoDB is not available)
     *
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteLocalArtifact(@NotNull final Long id) {
        final LocalArtifact existing = localArtifactRepository.findOne(id);

        if (null == existing) {
            return;
        }

        deleteGridFsArtifact(existing);

        existing.getSoftwareModule().removeArtifact(existing);
        softwareModuleRepository.save(existing.getSoftwareModule());
        localArtifactRepository.delete(id);
    }

    /**
     * Delete a grid fs file.
     *
     * @param existing
     *            the related local artifact
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteGridFsArtifact(@NotNull final LocalArtifact existing) {
        if (existing == null) {
            return;
        }

        boolean artifactIsOnlyUsedByOneSoftwareModule = true;
        for (LocalArtifact lArtifact : localArtifactRepository.findByGridFsFileName(existing.getGridFsFileName())) {
            if (!lArtifact.getSoftwareModule().isDeleted()
                    && lArtifact.getSoftwareModule().getId() != existing.getSoftwareModule().getId()) {
                artifactIsOnlyUsedByOneSoftwareModule = false;
                break;
            }
        }

        if (artifactIsOnlyUsedByOneSoftwareModule) {
            try {
                LOG.debug("deleting artifact from repository {}", existing.getGridFsFileName());
                artifactRepository.deleteBySha1(existing.getGridFsFileName());
            } catch (final ArtifactStoreException e) {
                throw new ArtifactDeleteFailedException(e);
            }
        }
    }

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id
     *            of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException
     *             if deletion failed (MongoDB is not available)
     *
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    public void deleteExternalArtifact(@NotNull final Long id) {
        final ExternalArtifact existing = externalArtifactRepository.findOne(id);

        if (null == existing) {
            return;
        }

        existing.getSoftwareModule().removeArtifact(existing);
        softwareModuleRepository.save(existing.getSoftwareModule());
        externalArtifactRepository.delete(id);
    }

    /**
     * Searches for {@link Artifact} with given file name.
     *
     * @param filename
     *            to search for
     * @return found List of {@link LocalArtifact}s.
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public List<LocalArtifact> findLocalArtifactByFilename(@NotNull final String filename) {
        return localArtifactRepository.findByFilename(filename);
    }

    /**
     * Searches for {@link Artifact} with given {@link Identifiable}.
     *
     * @param id
     *            to search for
     * @return found {@link Artifact} or <code>null</code> is it could not be
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Artifact findArtifact(@NotNull final Long id) {
        return localArtifactRepository.findOne(id);
    }

    /**
     * Loads {@link org.eclipse.hawkbit.artifact.server.json.model.Artifact}
     * from store for given {@link LocalArtifact}.
     *
     * @param artifact
     *            to search for
     * @return loaded
     *         {@link org.eclipse.hawkbit.artifact.server.json.model.Artifact}
     *
     * @throws GridFSDBFileNotFoundException
     *             if file could not be found in store
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DOWNLOAD_ARTIFACT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public DbArtifact loadLocalArtifactBinary(@NotNull final LocalArtifact artifact) {
        final DbArtifact result = artifactRepository.getArtifactBySha1(artifact.getGridFsFileName());
        if (result == null) {
            throw new GridFSDBFileNotFoundException(artifact.getGridFsFileName());
        }

        return result;
    }

    /**
     * Persists artifact binary as provided by given InputStream. assign the
     * artifact in addition to given {@link SoftwareModule}.
     *
     * @param inputStream
     *            to read from for artifact binary
     * @param moduleId
     *            to assign the new artifact to
     * @param filename
     *            of the artifact
     * @param overrideExisting
     *            to <code>true</code> if the artifact binary can be overdiden
     *            if it already exists
     * @param contentType
     *            the contentType of the file
     *
     * @return uploaded {@link LocalArtifact}
     *
     * @throw ArtifactUploadFailedException if upload failes
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting, final String contentType) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, contentType);
    }

    /**
     * Persists artifact binary as provided by given InputStream. assign the
     * artifact in addition to given {@link SoftwareModule}.
     *
     * @param inputStream
     *            to read from for artifact binary
     * @param moduleId
     *            to assign the new artifact to
     * @param filename
     *            of the artifact
     * @param overrideExisting
     *            to <code>true</code> if the artifact binary can be overdiden
     *            if it already exists
     *
     * @return uploaded {@link LocalArtifact}
     *
     * @throw ArtifactUploadFailedException if upload failes
     */
    @Modifying
    @Transactional
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    public LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, null);
    }

    /**
     * Get local artifact for a base software module.
     *
     * @param pageReq
     *            Pageable
     * @param swId
     *            software module id
     * @return Page<LocalArtifact>
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    public Page<LocalArtifact> findLocalArtifactBySoftwareModule(@NotNull final Pageable pageReq,
            @NotNull final Long swId) {
        return localArtifactRepository.findBySoftwareModuleId(pageReq, swId);
    }

    /**
     * Find by artifact by software module id and filename.
     *
     * @param filename
     *            file name
     * @param softwareModuleId
     *            software module id.
     * @return LocalArtifact if artifact present
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    public List<LocalArtifact> findByFilenameAndSoftwareModule(@NotNull final String filename,
            @NotNull final Long softwareModuleId) {
        return localArtifactRepository.findByFilenameAndSoftwareModuleId(filename, softwareModuleId);
    }
}
