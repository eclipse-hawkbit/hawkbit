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
     * Counts current elements based on the sha1 and tenant, as well as having
     * the {@link SoftwareModule} property 'deleted' with value 'false
     *
     * @param sha1
     *            the sha1 of the {@link Artifact}
     * @param tenant
     *            the current tenant
     * 
     * @return the count of the elements
     */
    long countBySha1HashAndTenantAndSoftwareModuleDeletedIsFalse(@Param("sha1") String sha1,
            @Param("tenant") String tenant);

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
     * @param softwareModuleId
     *            software module id
     *
     * @return Page<Artifact>
     */
    Page<Artifact> findBySoftwareModuleId(Pageable pageReq, Long softwareModuleId);

    /**
     * Count the artifacts that are associated with the given software module.
     * 
     * @param softwareModuleId
     *            software module ID
     * 
     * @return the current number of artifacts associated with the software
     *         module.
     */
    long countBySoftwareModuleId(Long softwareModuleId);

    /**
     * Searches for a {@link Artifact} based user provided filename at upload
     * and selected software module id.
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
