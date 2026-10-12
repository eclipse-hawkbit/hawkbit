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

    // The tenant identity is the trimmed upper case name (as the runtime tenant and JpaTenantMetaData#getTenant) - legacy tenants
    // could be stored in another case / with surrounding spaces, so all lookups compare trimmed upper case values.

    // find without details
    @Query("SELECT t FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    TenantMetaData findByTenantIgnoreCase(@Param("tenant") String tenant);

    // find with details
    @EntityGraph(value = "TenantMetaData.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    TenantMetaData findWitDetailsByTenantIgnoreCase(@Param("tenant") String tenant);

    @Query("SELECT UPPER(TRIM(t.tenant)) FROM JpaTenantMetaData t")
    Page<String> findTenants(final Pageable pageable);

    // the tenant as stored (JpaTenantMetaData#getTenant returns it normalized), null if there is no such tenant
    @Query("SELECT t.tenant FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    String findStoredTenantIgnoreCase(@Param("tenant") String tenant);

    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);
}
