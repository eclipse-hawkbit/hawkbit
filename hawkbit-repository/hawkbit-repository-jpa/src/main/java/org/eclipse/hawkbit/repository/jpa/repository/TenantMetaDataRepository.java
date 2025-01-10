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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for operations on {@link TenantMetaData} entity.
 */
@Transactional(readOnly = true)
public interface TenantMetaDataRepository extends PagingAndSortingRepository<JpaTenantMetaData, Long>, CrudRepository<JpaTenantMetaData, Long> {

    // find without details
    TenantMetaData findByTenantIgnoreCase(String tenant);

    // find with details
    @EntityGraph(value = "TenantMetaData.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    TenantMetaData findWitDetailsByTenantIgnoreCase(String tenant);

    @Query("SELECT  t.tenant FROM JpaTenantMetaData t")
    Page<String> findTenants(final Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(t.tenant) = UPPER(:tenant)")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);
}