/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusCode;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActionStatusCode} repository.
 */
@Transactional(readOnly = true)
public interface ActionStatusCodeRepository extends BaseEntityRepository<JpaActionStatusCode> {

    /**
     * Find an action status code by its numeric code.
     *
     * @param code the numeric code
     * @return the action status code if found
     */
    Optional<JpaActionStatusCode> findByCode(int code);

    /**
     * Check if an action status code with the given code exists.
     *
     * @param code the numeric code
     * @return true if exists
     */
    boolean existsByCode(int code);

    /**
     * Finds distinct non-null action status codes that have been reported by devices
     * but do not have a corresponding catalogue entry, ordered by code ascending.
     *
     * @param pageable the page request (sort is ignored)
     * @return page of undocumented codes
     */
    // Tenant filtering is applied implicitly to the main query and the subquery (EclipseLink @Multitenant / Hibernate @TenantId).
    @Query(value = "SELECT DISTINCT s.code FROM JpaActionStatus s WHERE s.code IS NOT NULL "
            + "AND NOT EXISTS (SELECT c.id FROM JpaActionStatusCode c WHERE c.code = s.code) ORDER BY s.code",
           countQuery = "SELECT COUNT(DISTINCT s.code) FROM JpaActionStatus s WHERE s.code IS NOT NULL "
            + "AND NOT EXISTS (SELECT c.id FROM JpaActionStatusCode c WHERE c.code = s.code)")
    Page<Integer> findUndocumentedCodes(Pageable pageable);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaActionStatusCode a WHERE a.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
