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

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * repository for operations on {@link TenantMetaData} entity.
 */
@Transactional(readOnly = true)
public interface TenantMetaDataRepository
        extends PagingAndSortingRepository<JpaTenantMetaData, Long>,
        CrudRepository<JpaTenantMetaData, Long> {

    /**
     * Search {@link TenantMetaData} by tenant name.
     *
     * @param tenant to search for
     * @return found {@link TenantMetaData} or <code>null</code>
     */
    TenantMetaData findByTenantIgnoreCase(String tenant);

    @Transactional
    @Query("SELECT  t.tenant FROM JpaTenantMetaData t")
    Page<String> findTenants(final Pageable pageable);

    /**
     * @param tenant
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(t.tenant) = UPPER(:tenant)")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);
}
