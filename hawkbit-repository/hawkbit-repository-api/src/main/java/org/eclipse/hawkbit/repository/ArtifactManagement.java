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
import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.repository.model.AbstractDbArtifact;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Identifiable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for {@link Artifact} management operations.
 *
 */
public interface ArtifactManagement {
    /**
     * @return the total amount of local artifacts stored in the artifact
     *         management
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long count();

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
     * @return uploaded {@link Artifact}
     *
     * @throws ArtifactUploadFailedException
     *             if upload fails
     * @throws EntityNotFoundException
     *             if given software module does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    Artifact create(@NotNull InputStream inputStream, long moduleId, final String filename,
            final boolean overrideExisting);

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
     *            to <code>true</code> if the artifact binary can be overridden
     *            if it already exists
     * @param contentType
     *            the contentType of the file
     * @return uploaded {@link Artifact}
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
    Artifact create(@NotNull InputStream stream, long moduleId, @NotEmpty String filename, String providedMd5Sum,
            String providedSha1Sum, boolean overrideExisting, String contentType);

    /**
     * Garbage collects artifact binaries if only referenced by given
     * {@link SoftwareModule#getId()} or {@link SoftwareModules} that are marged
     * as deleted.
     * 
     *
     * @param artifactSha1Hash
     *            no longer needed
     * @param moduleId
     *            the garbage colelction call is made for
     * 
     * @return <code>true</code> if an binary was actually garbage collected
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    boolean clearArtifactBinary(@NotEmpty String artifactSha1Hash, long moduleId);

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id
     *            of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException
     *             if deletion failed (MongoDB is not available)
     * @throws EntityNotFoundException
     *             if artifact with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(long id);

    /**
     * Searches for {@link Artifact} with given {@link Identifiable}.
     *
     * @param id
     *            to search for
     * @return found {@link Artifact}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> get(long id);

    /**
     * Find by artifact by software module id and filename.
     *
     * @param filename
     *            file name
     * @param softwareModuleId
     *            software module id.
     * @return found {@link Artifact}
     * 
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> getByFilenameAndSoftwareModule(@NotNull String filename, long softwareModuleId);

    /**
     * Find all local artifact by sha1 and return the first artifact.
     *
     * @param sha1
     *            the sha1
     * @return the first local artifact
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> findFirstBySHA1(@NotNull String sha1);

    /**
     * Searches for {@link Artifact} with given file name.
     *
     * @param filename
     *            to search for
     * @return found List of {@link Artifact}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> getByFilename(@NotNull String filename);

    /**
     * Get local artifact for a base software module.
     *
     * @param pageReq
     *            Pageable parameter
     * @param swId
     *            software module id
     * @return Page<Artifact>
     * 
     * @throws EntityNotFoundException
     *             if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<Artifact> findBySoftwareModule(@NotNull Pageable pageReq, long swId);

    /**
     * Loads {@link AbstractDbArtifact} from store for given {@link Artifact}.
     *
     * @param sha1Hash
     *            to search for
     * @return loaded {@link AbstractDbArtifact}
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DOWNLOAD_ARTIFACT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<AbstractDbArtifact> loadArtifactBinary(@NotEmpty String sha1Hash);

}
