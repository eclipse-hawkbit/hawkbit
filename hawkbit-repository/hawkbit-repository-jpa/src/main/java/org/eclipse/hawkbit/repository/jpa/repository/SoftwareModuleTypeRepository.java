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

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link SoftwareModuleType}.
 */
@Transactional(readOnly = true)
public interface SoftwareModuleTypeRepository extends BaseEntityRepository<JpaSoftwareModuleType> {

    /**
     * @param key to search for
     * @return all {@link SoftwareModuleType}s in the repository with given {@link SoftwareModuleType#getKey()}
     */
    Optional<SoftwareModuleType> findByKey(String key);

    /**
     * @param name to search for
     * @return all {@link SoftwareModuleType}s in the repository with given {@link SoftwareModuleType#getName()}
     */
    Optional<SoftwareModuleType> findByName(String name);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety reasons (this is a "delete everything" query
     * after all) we add the tenant manually to query even if this is done by {@link EntityManager} anyhow. The DB should take
     * care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaSoftwareModuleType t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}