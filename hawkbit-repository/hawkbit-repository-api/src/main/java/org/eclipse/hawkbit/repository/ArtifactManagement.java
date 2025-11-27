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

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.artifact.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.artifact.model.ArtifactStream;
import org.eclipse.hawkbit.artifact.model.StoredArtifactInfo;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMd5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSha1HashException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for {@link Artifact} management operations.
 */
public interface ArtifactManagement extends PermissionSupport {

    @Override
    default String permissionGroup() {
        return SpPermission.SOFTWARE_MODULE;
    }

    /**
     * Persists artifact binary as provided by given InputStream. assign the
     * artifact in addition to given {@link SoftwareModule}.
     *
     * @param artifactUpload {@link ArtifactUpload} containing the upload information
     * @return uploaded {@link Artifact}
     * @throws EntityNotFoundException if given software module does not exist
     * @throws EntityAlreadyExistsException if File with that name already exists in the Software Module
     * @throws ArtifactUploadFailedException if upload fails with internal server errors
     * @throws InvalidMd5HashException if check against provided MD5 checksum failed
     * @throws InvalidSha1HashException if check against provided SHA1 checksum failed
     * @throws ConstraintViolationException if {@link ArtifactUpload} contains invalid values
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    Artifact create(@NotNull @Valid ArtifactUpload artifactUpload);

    /**
     * Loads {@link StoredArtifactInfo} from store for given {@link Artifact}.
     *
     * @param sha1Hash to search for
     * @param softwareModuleId software module id.
     * @param isEncrypted flag to indicate if artifact is encrypted.
     * @return loaded {@link StoredArtifactInfo}
     */
    @PreAuthorize("hasAuthority('" + SpPermission.READ_SOFTWARE_MODULE_ARTIFACT + "')" + " or " + SpringEvalExpressions.IS_CONTROLLER)
    ArtifactStream getArtifactStream(@NotEmpty String sha1Hash, long softwareModuleId, final boolean isEncrypted);

    /**
     * Deletes {@link Artifact} based on given id.
     *
     * @param id of the {@link Artifact} that has to be deleted.
     * @throws EntityNotFoundException if artifact with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(long id);
}