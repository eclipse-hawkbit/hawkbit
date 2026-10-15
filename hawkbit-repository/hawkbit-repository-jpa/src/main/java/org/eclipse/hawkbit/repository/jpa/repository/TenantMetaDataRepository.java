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

import java.util.List;

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.cache.annotation.Cacheable;
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

    String CACHE_TENANT_SOFT_DELETED = "tenantSoftDeleted";

    // The tenant identity is the trimmed upper case name (as the runtime tenant and JpaTenantMetaData#getTenant) - legacy tenants
    // could be stored in another case / with surrounding spaces, so all lookups compare trimmed upper case values.

    // find without details
    @Query("SELECT t FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    TenantMetaData findByTenant(@Param("tenant") String tenant);

    // find with details
    @EntityGraph(value = "TenantMetaData.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    TenantMetaData findWitDetailsByTenant(@Param("tenant") String tenant);

    // all tenants, including the soft deleted ones
    @Query("SELECT UPPER(TRIM(t.tenant)) FROM JpaTenantMetaData t")
    Page<String> findAllTenants(Pageable pageable);

    // tenants not soft deleted at the given time - active or with a pending (future) soft delete
    @Query("SELECT UPPER(TRIM(t.tenant)) FROM JpaTenantMetaData t WHERE t.deletedAt IS NULL OR t.deletedAt > :now")
    Page<String> findActiveTenants(@Param("now") long now, Pageable pageable);

    // the tenant as stored (JpaTenantMetaData#getTenant returns it normalized), null if there is no such tenant
    @Query("SELECT t.tenant FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    String findStoredTenant(@Param("tenant") String tenant);

    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    void deleteByTenant(@Param("tenant") String tenant);

    // the soft delete time of the tenant, null if not soft deleted or there is no such tenant.
    // Null is cached too (hawkbit.cache.tenantSoftDeleted.cache-null=true).
    @Cacheable(value = CACHE_TENANT_SOFT_DELETED, key = "#tenant")
    @Query("SELECT t.deletedAt FROM JpaTenantMetaData t WHERE UPPER(TRIM(t.tenant)) = UPPER(TRIM(:tenant))")
    Long getDeletedAt(@Param("tenant") String tenant);

    // soft deleted tenants due for hard delete at the given time - only soft deleted tenants have a hard delete time and it is
    // never before the soft delete time (enforced on soft delete), so a tenant with a pending soft delete is never returned
    @Query("SELECT UPPER(TRIM(t.tenant)) FROM JpaTenantMetaData t WHERE t.hardDeleteAt <= :now")
    List<String> findTenantsToHardDelete(@Param("now") long now);
}
