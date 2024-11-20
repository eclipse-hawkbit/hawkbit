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

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The spring-data repository for the entity {@link TenantConfiguration}.
 */
@Transactional(readOnly = true)
public interface TenantConfigurationRepository extends BaseEntityRepository<JpaTenantConfiguration> {

    /**
     * Finds a specific {@link TenantConfiguration} by the configuration key.
     *
     * @param configurationKey the configuration key to find the configuration for
     * @return the found tenant configuration object otherwise {@code null}
     */
    JpaTenantConfiguration findByKey(String configurationKey);

    /**
     * Deletes a tenant configuration by tenant and key.
     *
     * @param tenant the tenant for this configuration
     * @param keyName the name of the key to be deleted
     */
    void deleteByKey(String keyName);

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
    @Query("DELETE FROM JpaTenantConfiguration t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

}
