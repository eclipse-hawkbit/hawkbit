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

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
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
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Identifiable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for {@link Artifact} management operations.
 *
 */
public interface ArtifactManagement {

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
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    ExternalArtifact createExternalArtifact(@NotNull ExternalArtifactProvider externalRepository, String urlSuffix,
            @NotNull Long moduleId);

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
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    ExternalArtifactProvider createExternalArtifactProvider(@NotEmpty String name, String description,
            @NotNull String basePath, String defaultUrlSuffix);

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
     *            to <code>true</code> if the artifact binary can be overridden
     *            if it already exists
     *
     * @return uploaded {@link LocalArtifact}
     *
     * @throw ArtifactUploadFailedException if upload fails
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    default LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, null);
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
     *            to <code>true</code> if the artifact binary can be overridden
     *            if it already exists
     * @param contentType
     *            the contentType of the file
     *
     * @return uploaded {@link LocalArtifact}
     *
     * @throw ArtifactUploadFailedException if upload fails
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    default LocalArtifact createLocalArtifact(final InputStream inputStream, final Long moduleId, final String filename,
            final boolean overrideExisting, final String contentType) {
        return createLocalArtifact(inputStream, moduleId, filename, null, null, overrideExisting, contentType);
    }

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
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    LocalArtifact createLocalArtifact(@NotNull InputStream stream, @NotNull Long moduleId, @NotEmpty String filename,
            String providedMd5Sum, String providedSha1Sum, boolean overrideExisting, String contentType);

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id
     *            of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException
     *             if deletion failed (MongoDB is not available)
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteExternalArtifact(@NotNull Long id);

    /**
     * Deletes a local artifact.
     *
     * @param existing
     *            the related local artifact
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteLocalArtifact(@NotNull LocalArtifact existing);

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id
     *            of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException
     *             if deletion failed (MongoDB is not available)
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteLocalArtifact(@NotNull Long id);

    /**
     * Searches for {@link Artifact} with given {@link Identifiable}.
     *
     * @param id
     *            to search for
     * @return found {@link Artifact} or <code>null</code> is it could not be
     *         found.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Artifact findArtifact(@NotNull Long id);

    /**
     * Find by artifact by software module id and filename.
     *
     * @param filename
     *            file name
     * @param softwareModuleId
     *            software module id.
     * @return LocalArtifact if artifact present
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<LocalArtifact> findByFilenameAndSoftwareModule(@NotNull String filename, @NotNull Long softwareModuleId);

    /**
     * Find all local artifact by sha1 and return the first artifact.
     *
     * @param sha1
     *            the sha1
     * @return the first local artifact
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    LocalArtifact findFirstLocalArtifactsBySHA1(@NotNull String sha1);

    /**
     * Searches for {@link Artifact} with given file name.
     *
     * @param filename
     *            to search for
     * @return found List of {@link LocalArtifact}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<LocalArtifact> findLocalArtifactByFilename(@NotNull String filename);

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
    Page<LocalArtifact> findLocalArtifactBySoftwareModule(@NotNull Pageable pageReq, @NotNull Long swId);

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
    SoftwareModule findSoftwareModuleById(@NotNull Long id);

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
    default SoftwareModule findSoftwareModuleWithDetails(@NotNull final Long id) {
        final SoftwareModule result = findSoftwareModuleById(id);
        if (result != null) {
            result.getArtifacts().size();
        }

        return result;
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
            + SpringEvalExpressions.HAS_CONTROLLER_DOWNLOAD)
    DbArtifact loadLocalArtifactBinary(@NotNull LocalArtifact artifact);

}