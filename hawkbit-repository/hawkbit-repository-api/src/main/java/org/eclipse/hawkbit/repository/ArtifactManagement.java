/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.ArtifactDeleteFailedException;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for {@link Artifact} management operations.
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
     * @param artifactUpload {@link ArtifactUpload} containing the upload information
     * @return uploaded {@link Artifact}
     * @throws EntityNotFoundException if given software module does not exist
     * @throws EntityAlreadyExistsException if File with that name already exists in the Software Module
     * @throws ArtifactUploadFailedException if upload fails with internal server errors
     * @throws InvalidMD5HashException if check against provided MD5 checksum failed
     * @throws InvalidSHA1HashException if check against provided SHA1 checksum failed
     * @throws ConstraintViolationException if {@link ArtifactUpload} contains invalid values
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    Artifact create(@NotNull @Valid ArtifactUpload artifactUpload);

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id of the {@link Artifact} that has to be deleted.
     * @throws ArtifactDeleteFailedException if deletion failed (MongoDB is not available)
     * @throws EntityNotFoundException if artifact with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(long id);

    /**
     * Searches for {@link Artifact} with given {@link Identifiable}.
     *
     * @param id to search for
     * @return found {@link Artifact}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> get(long id);

    /**
     * Find by artifact by software module id and filename.
     *
     * @param filename file name
     * @param softwareModuleId software module id.
     * @return found {@link Artifact}
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> getByFilenameAndSoftwareModule(@NotNull String filename, long softwareModuleId);

    /**
     * Find all local artifact by sha1 and return the first artifact.
     *
     * @param sha1 the sha1
     * @return the first local artifact
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> findFirstBySHA1(@NotNull String sha1);

    /**
     * Searches for {@link Artifact} with given file name.
     *
     * @param filename to search for
     * @return found List of {@link Artifact}s.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Artifact> getByFilename(@NotNull String filename);

    /**
     * Get local artifact for a base software module.
     *
     * @param pageReq Pageable parameter
     * @param softwareModuleId software module id
     * @return Page<Artifact>
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<Artifact> findBySoftwareModule(@NotNull Pageable pageReq, long softwareModuleId);

    /**
     * Count local artifacts for a base software module.
     *
     * @param softwareModuleId software module id
     * @return count by software module
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long countBySoftwareModule(long softwareModuleId);

    /**
     * Loads {@link DbArtifact} from store for given {@link Artifact}.
     *
     * @param sha1Hash to search for
     * @param softwareModuleId software module id.
     * @param isEncrypted flag to indicate if artifact is encrypted.
     * @return loaded {@link DbArtifact}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DOWNLOAD_ARTIFACT + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<DbArtifact> loadArtifactBinary(@NotEmpty String sha1Hash, long softwareModuleId,
            final boolean isEncrypted);

}
