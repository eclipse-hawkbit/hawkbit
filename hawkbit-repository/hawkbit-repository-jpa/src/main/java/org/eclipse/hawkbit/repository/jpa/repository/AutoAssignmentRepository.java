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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaAutoAssignment;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AutoAssignmentRepository extends BaseEntityRepository<JpaAutoAssignment> {

    /**
     * Find all auto assignments, associated with the given distribution sets
     * <p/>
     * No access control applied
     *
     * @param dsIds distribution set ids to be set to null
     * @return a list of the auto assignments
     */
    @Query("SELECT a FROM JpaAutoAssignment a WHERE a.distributionSet.id IN :ids")
    List<JpaAutoAssignment> findByDistributionSet(@Param("ids") Long... dsIds);

    /**
     * Batch-fetches the stored access control context for the given auto assignments. The context is a large, lazily
     * mapped {@code @Lob} that is intentionally not exposed on the entity, so the scheduler resolves it explicitly and
     * in one query per page to avoid the N+1 that per-entity lazy loading would cause.
     *
     * @param ids the auto assignment ids
     * @return map of auto assignment id to its serialized access control context; ids without a stored context are absent
     */
    default Map<Long, String> getAccessControlContexts(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        final Map<Long, String> result = HashMap.newHashMap(ids.size());
        for (final Object[] row : findAccessControlContexts(ids)) {
            result.put((Long) row[0], (String) row[1]);
        }
        return result;
    }

    @Query("SELECT a.id, a.accessControlContext FROM JpaAutoAssignment a WHERE a.id IN :ids AND a.accessControlContext IS NOT NULL")
    List<Object[]> findAccessControlContexts(@Param("ids") Collection<Long> ids);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety reasons (this is a "delete everything" query after all) we add
     * the tenant manually to query even if this will be done by {@link EntityManager} anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaAutoAssignment a WHERE a.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}