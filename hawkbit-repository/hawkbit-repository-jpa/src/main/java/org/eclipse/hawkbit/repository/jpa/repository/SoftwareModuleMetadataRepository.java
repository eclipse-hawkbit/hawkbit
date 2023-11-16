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

import java.util.Collection;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SoftwareModuleMetadata} repository.
 *
 */
@Transactional(readOnly = true)
public interface SoftwareModuleMetadataRepository
        extends PagingAndSortingRepository<JpaSoftwareModuleMetadata, SwMetadataCompositeKey>,
        CrudRepository<JpaSoftwareModuleMetadata, SwMetadataCompositeKey>,
        JpaSpecificationExecutor<JpaSoftwareModuleMetadata> {

    /**
     * Locates the meta data entries that match the given software module ID and
     * target visibility flag.
     * 
     * @param page
     *            The pagination parameters.
     * @param moduleId
     *            The ID of the software module.
     * @param targetVisible
     *            The target visibility flag.
     * 
     * @return A {@link Page} with the matching meta data entries.
     */
    Page<JpaSoftwareModuleMetadata> findBySoftwareModuleIdAndTargetVisible(Pageable page, Long moduleId,
            boolean targetVisible);

    /**
     * Locates the meta data entries that match the given software module IDs
     * and target visibility flag.
     * <p/>
     * No access control applied
     * 
     * @param page The pagination parameters.
     * @param moduleId List of software module IDs.
     * @param targetVisible The target visibility flag.
     * @return A {@link Page} with the matching meta data entries.
     */
    @Query("SELECT smd.softwareModule.id, smd FROM JpaSoftwareModuleMetadata smd WHERE smd.softwareModule.id IN :moduleId AND smd.targetVisible = :targetVisible")
    Page<Object[]> findBySoftwareModuleIdInAndTargetVisible(Pageable page, @Param("moduleId") Collection<Long> moduleId,
            @Param("targetVisible") boolean targetVisible);

    /**
     * Counts the meta data entries that are associated with the addressed
     * software module.
     * <p/>
     * No access control applied
     * 
     * @param moduleId The ID of the software module.
     * @return The number of meta data entries associated with the software module.
     */
    long countBySoftwareModuleId(@Param("moduleId") Long moduleId);
}
