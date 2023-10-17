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

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SoftwareModule} repository.
 *
 */
@Transactional(readOnly = true)
public interface SoftwareModuleRepository
        extends BaseEntityRepository<JpaSoftwareModule> {

    /**
     * Counts all {@link SoftwareModule}s based on the given {@link JpaSoftwareModuleType}.
     *
     * @param type
     *            to count for
     * @return number of {@link SoftwareModule}s
     */
    Long countByType(JpaSoftwareModuleType type);

    /**
     * Count the software modules which are assigned to the distribution set
     * with the given ID.
     * 
     * @param setId
     *            the distribution set ID
     * 
     * @return the number of software modules matching the given distribution
     *         set ID.
     */
    long countByAssignedToId(Long setId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaSoftwareModule t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
