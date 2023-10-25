/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Artifact} repository.
 *
 */
@Transactional(readOnly = true)
public interface LocalArtifactRepository
        extends BaseEntityRepository<JpaArtifact> {

    /**
     * Counts artifacts size where the related software module is not
     * deleted/archived.
     * <p/>
     * No access control applied.
     *
     * @return sum of artifacts size in bytes
     */
    @Query("SELECT SUM(la.size) FROM JpaArtifact la WHERE la.softwareModule.deleted = false")
    Optional<Long> sumOfNonDeletedArtifactSize();

    /**
     * Counts artifacts where the related software module is deleted/archived.
     * <p/>
     * No access control applied
     *
     * @param deleted to true for counting the deleted artifacts
     * @return number of artifacts
     */
    Long countBySoftwareModuleDeleted(boolean deleted);

    /**
     * Counts current elements based on the sha1 and tenant, as well as having the
     * {@link SoftwareModule} property 'deleted' with value 'false'
     * <p/>
     * No access control applied
     *
     * @param sha1 the sha1 of the {@link Artifact}
     * @param tenant the current tenant\
     * @return the count of the elements
     */
    long countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(
            @Param("sha1") String sha1, @Param("tenant") String tenant);

    /**
     * Searches for a {@link Artifact} based on given gridFsFileName.
     *
     * @param sha1Hash to search
     * @return {@link Artifact} the first in the result list
     */
    Optional<Artifact> findFirstBySha1Hash(String sha1Hash);

    /**
     * Searches for a {@link Artifact} based user provided filename at upload.
     *
     * @param filename to search
     * @return list of {@link Artifact}.
     */
    Optional<Artifact> findFirstByFilename(String filename);

    /**
     * Searches for a {@link Artifact} based user provided filename at upload and
     * selected software module id.
     *
     * @param filename
     *            to search
     * @param softwareModuleId
     *            selected software module id
     * 
     * @return list of {@link Artifact}.
     */
    Optional<Artifact> findFirstByFilenameAndSoftwareModuleId(String filename, Long softwareModuleId);
}
