/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Artifact} repository.
 *
 */
@Transactional(readOnly = true)
public interface LocalArtifactRepository extends BaseEntityRepository<JpaArtifact, Long> {

    /**
     * Counts artifacts size where the related software module is not
     * deleted/archived.
     *
     * @return sum of artifacts size in bytes
     */
    @Query("SELECT SUM(la.size) FROM JpaArtifact la WHERE la.softwareModule.deleted = 0")
    Optional<Long> getSumOfUndeletedArtifactSize();

    /**
     * Counts artifacts where the related software module is deleted/archived.
     *
     * @param deleted
     *            to true for counting the deleted artifacts
     *
     * @return number of artifacts
     */
    Long countBySoftwareModuleDeleted(boolean deleted);

    /**
     * Searches for a {@link Artifact} based on given gridFsFileName.
     *
     * @param sha1Hash
     *            to search
     * @return list of {@link Artifact}s.
     */
    List<Artifact> findBySha1Hash(String sha1Hash);

    /**
     * Verifies if an artifact exists that has given hash and is still related
     * to a {@link SoftwareModule} other than a given one and not
     * {@link SoftwareModule#isDeleted()}.
     * 
     * @param sha1
     *            to search for
     * @param moduleId
     *            to ignore in relationship check
     * 
     * @return <code>true</code> if such an artifact exists
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaArtifact a WHERE a.sha1Hash = :sha1 AND a.softwareModule.id != :moduleId AND a.softwareModule.deleted = 0")
    boolean existsWithSha1HashAndSoftwareModuleIdIsNot(@Param("sha1") String sha1, @Param("moduleId") Long moduleId);

    /**
     * Searches for a {@link Artifact} based on given gridFsFileName.
     *
     * @param sha1Hash
     *            to search
     * @return {@link Artifact} the first in the result list
     */
    Optional<Artifact> findFirstBySha1Hash(String sha1Hash);

    /**
     * Searches for a {@link Artifact} based user provided filename at upload.
     *
     * @param filename
     *            to search
     * @return list of {@link Artifact}.
     */
    Optional<Artifact> findFirstByFilename(String filename);

    /**
     * Searches for local artifact for a base software module.
     *
     * @param pageReq
     *            Pageable
     * @param swId
     *            software module id
     *
     * @return Page<Artifact>
     */
    Page<Artifact> findBySoftwareModuleId(Pageable pageReq, final Long swId);

    /**
     * Searches for a {@link Artifact} based user provided filename at upload
     * and selected software module id.
     *
     * @param filename
     *            to search
     * @param softwareModuleId
     *            selected software module id
     * @return list of {@link Artifact}.
     */
    Optional<Artifact> findFirstByFilenameAndSoftwareModuleId(final String filename, final Long softwareModuleId);

}
